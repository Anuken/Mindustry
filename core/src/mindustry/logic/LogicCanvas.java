package mindustry.logic;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.geom.*;
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

    private Seq<LogicNode> nodes = new Seq<>();

    {
        LogicElement e = new LogicElement();
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
    }

    static class LogicElement extends Table{
        LogicNode node;
        NodeField[] fields = {new NodeField(true, "input 1"), new NodeField(true, "input 2"), new NodeField(false, "output 1"), new NodeField(false, "output 2")};

        LogicElement(){
            background(Tex.whitePane);
            setColor(Pal.accent.cpy().mul(0.9f).shiftSaturation(-0.3f));
            touchable = Touchable.enabled;

            margin(0f);

            table(Tex.whiteui, t -> {
                t.update(() -> {
                    t.setColor(color);
                });

                t.margin(8f);

                t.add("Node").style(Styles.outlineLabel).color(color);
                t.add().growX();
                t.button(Icon.cancel, Styles.onlyi, () -> {

                });
            }).growX().padBottom(5);

            row();

            defaults().size(190, 36);

            for(NodeField field : fields){
                add(field).color(color);
                row();
            }

            marginBottom(5);

            addListener(new InputListener(){
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

    static class NodeField extends Table{
        boolean input;
        ImageButton button;

        NodeField(boolean input, String name){
            this.input = input;
            if(input){
                addIcon();
                left();
            }else{
                right();
            }

            add(name).padLeft(5).padRight(5).style(Styles.outlineLabel);

            if(!input){
                addIcon();
            }
        }

        void addIcon(){
            float s = 30f;
            Cell<ImageButton> c = button(Tex.logicNode, Styles.colori, () -> {

            }).size(s);

            button = c.get();
            c.update(i -> i.getStyle().imageUpColor = color);

            float pad = s/2f - 3f;

            if(input){
                c.padLeft(-pad);
            }else{
                c.padRight(-pad);
            }
        }

        @Override
        public void setColor(Color color){
            super.setColor(color);

            button.getStyle().imageUpColor = color;
        }
    }
}
