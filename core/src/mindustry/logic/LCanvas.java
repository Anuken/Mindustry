package mindustry.logic;

import arc.*;
import arc.func.*;
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
import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.logic.*;

public class LCanvas extends Table{
    private static final Color backgroundCol = Pal.darkMetal.cpy().mul(0.1f), gridCol = Pal.darkMetal.cpy().mul(0.5f);
    private static Seq<Runnable> postDraw = new Seq<>();
    private Vec2 offset = new Vec2();

    DragLayout statements;
    StatementElem dragging;
    ScrollPane pane;
    float targetWidth;

    public LCanvas(){
        rebuild();
    }

    /** @return if statement elements should have rows. */
    public static boolean useRows(){
        return Core.graphics.getWidth() < Scl.scl(900f) * 1.2f;
    }

    public void rebuild(){
        targetWidth = useRows() ? 400f : 900f;
        float s = pane != null ? pane.getScrollPercentY() : 0f;
        String toLoad = statements != null ? save() : null;

        clear();

        statements = new DragLayout();

        pane = pane(t -> {
            t.center();
            t.add(statements).pad(2f).center().width(targetWidth);
        }).grow().get();
        pane.setClip(false);
        pane.setFlickScroll(false);

        //load old scroll percent
        Core.app.post(() -> {
            pane.setScrollPercentY(s);
            pane.updateVisualScroll();
            pane.requestScroll();
        });

        if(toLoad != null){
            load(toLoad);
        }
    }

    void add(LStatement statement){
        statements.addChild(new StatementElem(statement));
    }

    String save(){
        Seq<LStatement> st = statements.getChildren().<StatementElem>as().map(s -> s.st);
        st.each(LStatement::saveUI);

        return LAssembler.write(st);
    }

    void load(String asm){
        Seq<LStatement> statements = LAssembler.read(asm);
        statements.truncate(LogicBlock.maxInstructions);
        this.statements.clearChildren();
        for(LStatement st : statements){
            add(st);
        }

        for(LStatement st : statements){
            st.setupUI();
        }

        this.statements.layout();
    }

    @Override
    public void draw(){
        postDraw.clear();
        super.draw();
        postDraw.each(Runnable::run);
    }

    public class DragLayout extends WidgetGroup{
        float space = Scl.scl(10f), prefWidth, prefHeight;
        Seq<Element> seq = new Seq<>();
        int insertPosition = 0;

        {
            setTransform(true);
        }

        @Override
        public void layout(){
            float cy = 0;
            seq.clear();

            float totalHeight = getChildren().sumf(e -> e.getHeight() + space);

            height = prefHeight = totalHeight;
            width = prefWidth = Scl.scl(targetWidth);

            //layout everything normally
            for(int i = 0; i < getChildren().size; i++){
                Element e = getChildren().get(i);

                //ignore the dragged element
                if(dragging == e) continue;

                e.setSize(width, e.getPrefHeight());
                e.setPosition(0, height - cy, Align.topLeft);

                cy += e.getPrefHeight() + space;
                seq.add(e);
            }

            //insert the dragged element if necessary
            if(dragging != null){
                //find real position of dragged element top
                float realY = dragging.getY(Align.top) + dragging.translation.y;

                insertPosition = 0;

                for(int i = 0; i < seq.size; i++){
                    Element cur = seq.get(i);
                    //find fit point
                    if(realY < cur.y && (i == seq.size - 1 || realY > seq.get(i + 1).y)){
                        insertPosition = i + 1;
                        break;
                    }
                }

                float shiftAmount = dragging.getHeight() + space;

                //shift elements below insertion point down
                for(int i = insertPosition; i < seq.size; i++){
                    seq.get(i).y -= shiftAmount;
                }
            }

            invalidateHierarchy();
        }

        @Override
        public float getPrefWidth(){
            return prefWidth;
        }

        @Override
        public float getPrefHeight(){
            return prefHeight;
        }

        @Override
        public void draw(){
            Draw.alpha(parentAlpha);

            //draw selection box indicating placement position
            if(dragging != null && insertPosition <= seq.size){
                float shiftAmount = dragging.getHeight();
                float lastX = x;
                float lastY = insertPosition == 0 ? height + y : seq.get(insertPosition - 1).y + y - space;

                Tex.pane.draw(lastX, lastY - shiftAmount, width, dragging.getHeight());
            }

            super.draw();
        }

        void finishLayout(){
            if(dragging != null){
                //reset translation first
                for(Element child : getChildren()){
                    child.setTranslation(0, 0);
                }
                clearChildren();

                //reorder things
                for(int i = 0; i <= insertPosition - 1 && i < seq.size; i++){
                    addChild(seq.get(i));
                }

                addChild(dragging);

                for(int i = insertPosition; i < seq.size; i++){
                    addChild(seq.get(i));
                }

                dragging = null;
            }

            layout();
        }
    }

    public class StatementElem extends Table{
        LStatement st;

        public StatementElem(LStatement st){
            this.st = st;
            st.elem = this;

            background(Tex.whitePane);
            setColor(st.category().color);
            margin(0f);
            touchable = Touchable.enabled;

            table(Tex.whiteui, t -> {
                t.color.set(color);
                t.addListener(new HandCursorListener());

                t.margin(6f);
                t.touchable = Touchable.enabled;

                t.add(st.name()).style(Styles.outlineLabel).color(color).padRight(8);
                t.add().growX();

                t.button(Icon.copy, Styles.logici, () -> {
                }).padRight(6).get().tapped(() -> copy());

                t.button(Icon.cancel, Styles.logici, () -> {
                    remove();
                    dragging = null;
                    statements.layout();
                });

                t.addListener(new InputListener(){
                    float lastx, lasty;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){

                        if(button == KeyCode.mouseMiddle){
                            copy();
                            return false;
                        }

                        Vec2 v = localToStageCoordinates(Tmp.v1.set(x, y));
                        lastx = v.x;
                        lasty = v.y;
                        dragging = StatementElem.this;
                        toFront();
                        statements.layout();
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer){
                        Vec2 v = localToStageCoordinates(Tmp.v1.set(x, y));

                        translation.add(v.x - lastx, v.y - lasty);
                        lastx = v.x;
                        lasty = v.y;

                        statements.layout();
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                        statements.finishLayout();
                    }
                });
            }).growX();

            row();

            table(t -> {
                t.left();
                t.marginLeft(4);
                t.setColor(color);
                st.build(t);
            }).pad(4).padTop(2).left().grow();

            marginBottom(7);
        }

        void copy(){
            LStatement copy = st.copy();
            if(copy != null){
                StatementElem s = new StatementElem(copy);

                statements.addChildAfter(StatementElem.this,s);
                statements.layout();
                copy.elem = s;
                copy.setupUI();
            }
        }

        @Override
        public void draw(){
            float pad = 5f;
            Fill.dropShadow(x + width/2f, y + height/2f, width + pad, height + pad, 10f, 0.9f * parentAlpha);

            Draw.color(0, 0, 0, 0.3f * parentAlpha);
            Fill.crect(x, y, width, height);
            Draw.reset();

            super.draw();
        }
    }

    public static class JumpButton extends ImageButton{
        @NonNull Prov<StatementElem> to;
        boolean selecting;
        float mx, my;

        public JumpButton(Color color, @NonNull Prov<StatementElem> getter, Cons<StatementElem> setter){
            super(Tex.logicNode, Styles.colori);

            to = getter;

            getStyle().imageUpColor = color;

            addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode code){
                    selecting = true;
                    setter.get(null);
                    mx = x;
                    my = y;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    mx = x;
                    my = y;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode code){
                    localToStageCoordinates(Tmp.v1.set(x, y));
                    StatementElem elem = hovered();

                    if(elem != null && !isDescendantOf(elem)){
                        setter.get(elem);
                    }else{
                        setter.get(null);
                    }
                    selecting = false;
                }
            });

            update(() -> {
                if(to.get() != null && to.get().parent == null){
                    setter.get(null);
                }
            });
        }

        @Override
        public void draw(){
            super.draw();

            postDraw.add(() -> {
                Element hover = to.get() == null && selecting ? hovered() : to.get();
                float tx = 0, ty = 0;
                boolean draw = false;
                //capture coordinates for use in lambda
                float rx = x + translation.x, ry = y + translation.y;

                Element p = parent;
                while(p != null){
                    rx += p.x + p.translation.x;
                    ry += p.y + p.translation.y;
                    p = p.parent;
                }

                if(hover != null){
                    tx = hover.getX(Align.right) + hover.translation.x;
                    ty = hover.getY(Align.right) + hover.translation.y;

                    Element op = hover.parent;
                    while(op != null){
                        tx += op.x + op.translation.x;
                        ty += op.y + op.translation.y;
                        op = op.parent;
                    }

                    draw = true;
                }else if(selecting){
                    tx = rx + mx;
                    ty = ry + my;
                    draw = true;
                }

                if(draw){
                    drawCurve(rx + width/2f, ry + height/2f, tx, ty, color);

                    float s = width;
                    Tex.logicNode.draw(tx + s*0.75f, ty - s/2f, -s, s);
                }
            });
        }

        StatementElem hovered(){
            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null){
                while(e != null && !(e instanceof StatementElem)){
                    e = e.parent;
                }
            }
            if(e == null || isDescendantOf(e)) return null;
            return (StatementElem)e;
        }

        void drawCurve(float x, float y, float x2, float y2, Color color){
            Lines.stroke(4f, color);
            Draw.alpha(parentAlpha);

            float dist = 100f;

            Lines.curve(
            x, y,
            x + dist, y,
            x2 + dist, y2,
            x2, y2,
            Math.max(20, (int)(Mathf.dst(x, y, x2, y2) / 5))
            );

            Draw.reset();
        }
    }
}
