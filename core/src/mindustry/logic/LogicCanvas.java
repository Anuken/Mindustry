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
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.LogicNode.*;
import mindustry.logic.LogicNodes.*;
import mindustry.logic.SavedLogic.*;
import mindustry.ui.*;

public class LogicCanvas extends WidgetGroup{
    private static final Color backgroundCol = Pal.darkMetal.cpy().mul(0.1f), gridCol = Pal.darkMetal.cpy().mul(0.5f);

    private Element selected;
    private Element entered;
    private Vec2 offset = new Vec2();

    {
        addListener(new InputListener(){
            float lastX, lastY;

            @Override
            public void touchDragged(InputEvent event, float mx, float my, int pointer){
                if(Core.app.isMobile() && pointer != 0) return;

                float dx = mx - lastX, dy = my - lastY;
                offset.add(dx, dy);

                for(Element e : getChildren()){
                    e.moveBy(dx, dy);
                }

                lastX = mx;
                lastY = my;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if((Core.app.isMobile() && pointer != 0) || (Core.app.isDesktop() && button != KeyCode.mouseMiddle)) return false;

                lastX = x;
                lastY = y;
                return true;
            }
        });

        //TODO debug stuff
        add(new BinaryOpNode());
        add(new BinaryOpNode());
        add(new BinaryOpNode());
        add(new NumberNode());
        add(new NumberNode());
        add(new NumberNode());
        add(new ConditionNode());
        add(new ConditionNode());
        add(new SignalNode());
        add(new SequenceNode());

        Log.info(JsonIO.print(JsonIO.write(save())));
    }

    private void add(LogicNode node){
        NodeElement e = new NodeElement(node);
        e.setPosition(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
        addChild(e);
        e.pack();
    }

    public SavedLogic save(){

        //convert elements to saved nodes for writing to JSON
        return new SavedLogic(getChildren().<NodeElement>as().map(e -> {
            SavedNode node = new SavedNode();
            node.state = e.node;
            node.x = e.x;
            node.y = e.y;
            node.connections = new SavedConnection[e.slots.length];

            for(int i = 0; i < e.slots.length; i++){
                SavedConnection con = (node.connections[i] = new SavedConnection());
                SlotElement slot = e.slots[i];

                if(slot.connection != null){
                    SlotElement to = slot.connection;

                    con.node = getChildren().indexOf(to.node);
                    con.slot = Structs.indexOf(to.node.slots, to);
                }else{
                    con.node = -1;
                }
            }

            return node;
        }).toArray(SavedNode.class));
    }

    public void load(SavedLogic state){
        clear();

        //add nodes as children first to make sure they can be accessed.
        for(SavedNode node : state.nodes){
            NodeElement elem = new NodeElement(node.state);
            elem.setPosition(node.x, node.y);
            addChild(elem);
        }

        //assign connection data
        for(int i = 0; i < state.nodes.length; i++){
            SavedNode node = state.nodes[i];
            NodeElement elem = (NodeElement)getChildren().get(i);

            for(int j = 0; j < node.connections.length; j++){
                SavedConnection con = node.connections[j];
                if(con.node >= 0 && con.node < state.nodes.length){
                    SlotElement slot = elem.slots[j];
                    slot.connection = ((NodeElement)getChildren().get(con.node)).slots[con.slot];
                }
            }

            elem.pack();
        }
    }

    @Override
    public void draw(){
        Draw.color(backgroundCol);

        Fill.crect(x, y, width, height);

        Draw.color(gridCol);

        float spacing = Scl.scl(50f);
        int xbars = (int)(width / spacing) + 2, ybars = (int)(width / spacing) + 2;
        float ox = offset.x % spacing, oy = offset.y % spacing;

        Lines.stroke(Scl.scl(3f));

        for(int i = 0; i < xbars; i++){
            float cx = x + width/2f + (i - xbars/2) * spacing + ox;
            Lines.line(cx, y, cx, y + height);
        }

        for(int i = 0; i < ybars; i++){
            float cy = y + height/2f + (i - ybars/2) * spacing + oy;
            Lines.line(0, cy, x + width, cy);
        }

        Draw.reset();

        super.draw();

        for(Element e : getChildren()){
            if(e instanceof NodeElement){
                NodeElement l = (NodeElement)e;

                for(SlotElement field : l.slots){
                    field.drawConnection();
                }
            }
        }

        if(selected != null){
            SlotElement field = (SlotElement)selected.userObject;
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

    class NodeElement extends Table{
        final LogicNode node;
        SlotElement[] slots;
        Table slotTable;

        NodeElement(LogicNode node){
            this.node = node;

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

            table(t -> slotTable = t).fill();

            rebuildSlots();

            marginBottom(7);
        }

        void rebuildSlots(){
            slotTable.clear();
            slotTable.defaults().height(30);

            NodeSlot[] nslots = node.slots();

            this.slots = new SlotElement[nslots.length];
            for(int i = 0; i < nslots.length; i++){
                this.slots[i] = new SlotElement(this, nslots[i]);
            }

            for(SlotElement field : slots){
                slotTable.add(field).growX().align(field.slot.input ? Align.left : Align.right);
                slotTable.row();
            }

            pack();
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

    class SlotElement extends Table{
        final NodeSlot slot;
        final NodeElement node;

        ImageButton button;
        SlotElement connection;

        SlotElement(NodeElement node, NodeSlot slot){
            this.slot = slot;
            this.node = node;

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
                ImageButton cb = connection.button;
                Vec2 from = localToStageCoordinates(Tmp.v2.set(button.x + button.getWidth()/2f, button.y + button.getHeight()/2f));
                Vec2 to = cb.localToStageCoordinates(Tmp.v1.set(cb.getWidth()/2f, cb.getHeight()/2f));

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

                    if(selected == null && !slot.input){
                        selected = button;
                        return true;
                    }

                    return false;
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                    entered = button;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode code){
                    localToStageCoordinates(Tmp.v1.set(x, y));
                    Element element = entered;

                    if(element != null && element.userObject instanceof SlotElement){
                        SlotElement field = (SlotElement)element.userObject;
                        //make sure inputs are matched to outputs, and that slot types match
                        if(field != SlotElement.this && field.slot.input != slot.input && field.slot.type == slot.type){
                            connection = field;
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
