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
import mindustry.ui.*;

public class LogicCanvas extends WidgetGroup{
    private static final Color backgroundCol = Color.black, gridCol = Pal.accent.cpy().mul(0.2f);
    private static final Color outCol = Pal.place, inCol = Pal.remove;

    private Element selected;
    private Element entered;
    private Seq<LogicNode> nodes = new Seq<>();

    {
        for(int i = 0; i < 3; i++){
            LogicElement e = new LogicElement();
            e.setPosition(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
            addChild(e);
            e.pack();
        }
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

                for(NodeField field : l.fields){
                    field.drawConnection();
                }
            }
        }

        if(selected != null){
            NodeField field = (NodeField)selected.userObject;
            Vec2 dest = selected.localToStageCoordinates(Tmp.v1.set(selected.getWidth()/2f, selected.getHeight()/2f));
            Vec2 mouse = Core.input.mouse();
            drawCurve(dest.x, dest.y, mouse.x, mouse.y, field.color);
        }
    }

    void drawCurve(float x, float y, float x2, float y2, Color color){
        Lines.stroke(4f, color);
        Lines.curve(
        x, y,
        x2, y,
        x, y2,
        x2, y2,
        Math.max(3, (int)(Mathf.dst(x, y, x2, y2) / 5))
        );

        Draw.reset();
    }

    class LogicElement extends Table{
        LogicNode node;
        NodeField[] fields = {new NodeField(true, "input 1"), new NodeField(true, "input 2"), new NodeField(false, "output 1"), new NodeField(false, "output 2")};

        LogicElement(){
            background(Tex.whitePane);
            setColor(Pal.accent.cpy().mul(0.9f).shiftSaturation(-0.3f));
            margin(0f);

            table(Tex.whiteui, t -> {
                t.update(() -> {
                    t.setColor(color);
                });

                t.margin(8f);
                t.touchable = Touchable.enabled;

                t.add("Node").style(Styles.outlineLabel).color(color);
                t.add().growX();
                t.button(Icon.cancel, Styles.onlyi, () -> {
                    //TODO disconnect things
                    remove();
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

            defaults().height(30);

            for(NodeField field : fields){
                add(field).align(field.input ? Align.left : Align.right);
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

    class NodeField extends Table{
        boolean input;
        ImageButton button;
        Element connection;

        NodeField(boolean input, String name){
            this.input = input;
            //TODO color should depend on data type
            setColor(outCol);

            float marg = 24f;

            if(input){
                addIcon();
                left();
                marginRight(marg);
            }else{
                right();
                marginLeft(marg);
            }

            add(name).padLeft(5).padRight(5).style(Styles.outlineLabel).color(color);

            if(!input){
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

            if(input){
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

                    if(element != null && element.userObject instanceof NodeField){
                        NodeField field = (NodeField)element.userObject;
                        if(field != NodeField.this && field.input != input){
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
