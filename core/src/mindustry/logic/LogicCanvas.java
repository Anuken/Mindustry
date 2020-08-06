package mindustry.logic;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.LogicNode.*;
import mindustry.ui.*;

public class LogicCanvas extends WidgetGroup{
    private static final Color backgroundCol = Pal.darkMetal.cpy().mul(0.1f), gridCol = Pal.darkMetal.cpy().mul(0.5f);

    private Element selected;
    private Element entered;
    private Seq<LogicNode> nodes = new Seq<>();

    {
        add(new BinaryOpNode());
        add(new BinaryOpNode());
        add(new BinaryOpNode());
        add(new NumberNode());
        add(new NumberNode());
        add(new NumberNode());
        add(new ConditionNode());
        add(new ConditionNode());
        add(new SignalNode());
    }

    private void add(LogicNode node){
        LogicElement e = new LogicElement(node);
        e.setPosition(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
        addChild(e);
        e.pack();
    }

    @Override
    public void draw(){
        Draw.color(backgroundCol);

        Fill.crect(x, y, width, height);

        Draw.color(gridCol);

        float spacing = Scl.scl(50f);
        int xbars = (int)(width / spacing) + 1, ybars = (int)(width / spacing) + 1;

        Lines.stroke(Scl.scl(3f));

        for(int i = 0; i < xbars; i++){
            float cx = x + width/2f + (i - xbars/2) * spacing;
            Lines.line(cx, y, cx, y + height);
        }

        for(int i = 0; i < ybars; i++){
            float cy = y + height/2f + (i - ybars/2) * spacing;
            Lines.line(0, cy, x + width, cy);
        }

        Draw.reset();

        super.draw();

        for(Element e : getChildren()){
            if(e instanceof LogicElement){
                LogicElement l = (LogicElement)e;

                for(SlotTable field : l.slots){
                    field.drawConnection();
                }
            }
        }

        if(selected != null){
            SlotTable field = (SlotTable)selected.userObject;
            Vec2 dest = selected.localToStageCoordinates(Tmp.v1.set(selected.getWidth()/2f, selected.getHeight()/2f));
            Vec2 mouse = Core.input.mouse();
            drawCurve(dest.x, dest.y, mouse.x, mouse.y, field.color);
        }
    }

    void drawCurve(float x, float y, float x2, float y2, Color color){
        Lines.stroke(4f, color);

        float dist = Math.abs(x - x2)/2f;

        Lines.curve(
        x, y,
        x + dist, y,
        x2 - dist, y2,
        x2, y2,
        Math.max(3, (int)(Mathf.dst(x, y, x2, y2) / 5))
        );

        Draw.reset();
    }

    class LogicElement extends Table{
        final LogicNode node;
        final SlotTable[] slots;

        LogicElement(LogicNode node){
            this.node = node;
            nodes.add(node);

            NodeSlot[] nslots = node.slots();

            this.slots = new SlotTable[nslots.length];
            for(int i = 0; i < nslots.length; i++){
                this.slots[i] = new SlotTable(nslots[i]);
            }

            background(Tex.whitePane);
            setColor(node.category().color);
            margin(0f);

            table(Tex.whiteui, t -> {
                t.update(() -> {
                    t.setColor(color);
                });

                t.margin(8f);
                t.touchable = Touchable.enabled;

                t.add(node.name()).style(Styles.outlineLabel).color(color).padRight(8);
                t.add().growX();
                t.button(Icon.cancel, Styles.onlyi, () -> {
                    //TODO disconnect things
                    remove();
                    nodes.remove(node);
                });
                t.addListener(new InputListener(){
                    float lastx, lasty;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        Vec2 v = localToStageCoordinates(Tmp.v1.set(x, y));
                        lastx = v.x;
                        lasty = v.y;
                        toFront();
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer){
                        Vec2 v = localToStageCoordinates(Tmp.v1.set(x, y));

                        moveBy(v.x - lastx, v.y - lasty);
                        lastx = v.x;
                        lasty = v.y;
                    }
                });
            }).growX().padBottom(6);

            row();

            node.build(this);

            row();

            defaults().height(30);

            for(SlotTable field : slots){
                add(field).align(field.slot.input ? Align.left : Align.right);
                row();
            }

            marginBottom(7);
        }

        @Override
        public void draw(){
            float pad = 10f;
            Fill.dropShadow(x + width/2f, y + height/2f, width + pad, height + pad, 20f, 0.95f);

            Draw.color(0, 0, 0, 0.3f);
            Fill.crect(x, y, width, height);
            Draw.reset();

            super.draw();
        }
    }

    class SlotTable extends Table{
        final NodeSlot slot;

        ImageButton button;
        Element connection;

        SlotTable(NodeSlot slot){
            this.slot = slot;

            setColor(slot.type.color);

            float marg = 24f;

            if(slot.input){
                addIcon();
                left();
                marginRight(marg);
            }else{
                right();
                marginLeft(marg);
            }

            add(slot.name).padLeft(5).padRight(5).style(Styles.outlineLabel).color(color);

            if(!slot.input){
                addIcon();
            }
        }

        void drawConnection(){
            if(connection != null){
                Vec2 from = localToStageCoordinates(Tmp.v2.set(button.getX() + button.getWidth()/2f, button.getY() + button.getHeight()/2f));
                Vec2 to = connection.localToStageCoordinates(Tmp.v1.set(connection.getWidth()/2f, connection.getHeight()/2f));

                drawCurve(from.x, from.y, to.x, to.y, color);
            }
        }

        void addIcon(){
            float s = 30f;
            Cell<ImageButton> c = button(Tex.logicNode, Styles.colori, () -> {

            }).size(s);

            button = c.get();
            button.userObject = this;
            button.getStyle().imageUpColor = color;

            float pad = s/2f - 3f;

            if(slot.input){
                c.padLeft(-pad);
            }else{
                c.padRight(-pad);
            }

            button.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode code){
                    if(selected == null){
                        selected = button;
                    }
                    return true;
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                    entered = button;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode code){
                    localToStageCoordinates(Tmp.v1.set(x, y));
                    Element element = entered;

                    if(element != null && element.userObject instanceof SlotTable){
                        SlotTable field = (SlotTable)element.userObject;
                        //make sure inputs are matched to outputs, and that slot types match
                        if(field != SlotTable.this && field.slot.input != slot.input && field.slot.type == slot.type){
                            connection = element;
                            //field.connection = button;
                        }
                    }

                    if(selected == button){
                        selected = null;
                    }
                }
            });
        }
    }
}
