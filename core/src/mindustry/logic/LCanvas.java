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
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.LStatements.*;
import mindustry.ui.*;

public class LCanvas extends Table{
    private static final Seq<JumpCurve> tmpOccupiers1 = new Seq<>();
    private static final Seq<JumpCurve> tmpOccupiers2 = new Seq<>();
    private static final Bits tmpBits1 = new Bits();
    private static final Bits tmpBits2 = new Bits();
    private static final int invalidJump = Integer.MAX_VALUE; // terrible hack
    //ew static variables
    static LCanvas canvas;
    private static final boolean dynamicJumpHeights = true;

    public DragLayout statements;
    public ScrollPane pane;

    StatementElem dragging;
    StatementElem hovered;
    float targetWidth;
    boolean privileged;
    Seq<Tooltip> tooltips = new Seq<>();

    public LCanvas(){
        canvas = this;

        Core.scene.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                //hide tooltips on tap
                for(var t : tooltips){
                    t.container.toFront();
                }
                Core.app.post(() -> {
                    tooltips.each(Tooltip::hide);
                    tooltips.clear();
                });
                return super.touchDown(event, x, y, pointer, button);
            }
        });

        rebuild();
    }

    /** @return if statement elements should have rows. */
    public static boolean useRows(){
        return Core.graphics.getWidth() < Scl.scl(900f) * 1.2f;
    }

    public static void tooltip(Cell<?> cell, String key){
        String lkey = key.toLowerCase().replace(" ", "");
        if(Core.settings.getBool("logichints", true) && Core.bundle.has(lkey)){
            var tip = new Tooltip(t -> t.background(Styles.black8).margin(4f).add("[lightgray]" + Core.bundle.get(lkey)).style(Styles.outlineLabel));

            //mobile devices need long-press tooltips
            if(Vars.mobile){
                cell.get().addListener(new ElementGestureListener(20, 0.4f, 0.43f, 0.15f){
                    @Override
                    public boolean longPress(Element element, float x, float y){
                        tip.show(element, x, y);
                        canvas.tooltips.add(tip);
                        //prevent touch down for other listeners
                        for(var list : cell.get().getListeners()){
                            if(list instanceof ClickListener cl){
                                cl.cancel();
                            }
                        }
                        return true;
                    }
                });
            }else{
                cell.get().addListener(tip);
            }

        }
    }

    public static void tooltip(Cell<?> cell, Enum<?> key){
        String cl = key.getClass().getSimpleName().toLowerCase() + "." + key.name().toLowerCase();
        if(Core.bundle.has(cl)){
            tooltip(cell, cl);
        }else{
            tooltip(cell, "lenum." + key.name());
        }
    }

    public void rebuild(){
        targetWidth = useRows() ? 400f : 900f;
        float s = pane != null ? pane.getVisualScrollY() : 0f;
        String toLoad = statements != null ? save() : null;

        clear();

        statements = new DragLayout();

        pane = pane(t -> {
            t.center();
            t.add(statements).pad(2f).center().width(targetWidth);
            t.addChild(statements.jumps);

            statements.jumps.touchable = Touchable.disabled;
            statements.jumps.update(() -> statements.jumps.setCullingArea(t.getCullingArea()));
            statements.jumps.cullable = false;
        }).grow().get();
        pane.setFlickScroll(false);
        pane.setScrollYForce(s);

        if(toLoad != null){
            load(toLoad);
        }
    }

    public void add(LStatement statement){
        statements.addChild(new StatementElem(statement));
    }

    public void addAt(int at, LStatement statement){
        statements.addChildAt(at, new StatementElem(statement));
    }

    public String save(){
        Seq<LStatement> st = statements.getChildren().<StatementElem>as().map(s -> s.st);
        st.each(LStatement::saveUI);

        return LAssembler.write(st);
    }

    public void load(String asm){
        statements.jumps.clear();

        Seq<LStatement> statements = LAssembler.read(asm, privileged);
        statements.truncate(LExecutor.maxInstructions);
        this.statements.clearChildren();
        for(LStatement st : statements){
            add(st);
        }

        for(LStatement st : statements){
            st.setupUI();
        }

        this.statements.updateJumpHeights = true;
    }

    public void clearStatements(){
        statements.jumps.clear();
        statements.clearChildren();
    }

    StatementElem checkHovered(){
        Element e = Core.scene.getHoverElement();
        if(e != null){
            while(e != null && !(e instanceof StatementElem)){
                e = e.parent;
            }
        }
        if(e == null || isDescendantOf(e)) return null;
        return (StatementElem)e;
    }

    @Override
    public void act(float delta){
        super.act(delta);

        hovered = checkHovered();

        if(Core.input.isTouched()){
            float y = Core.input.mouseY();
            float dst = Math.min(y - this.y, Core.graphics.getHeight() - y);
            if(dst < Scl.scl(100f)){ //scroll margin
                int sign = Mathf.sign(Core.graphics.getHeight()/2f - y);
                pane.setScrollY(pane.getScrollY() + sign * Scl.scl(15f) * Time.delta);
            }
        }
    }

    public class DragLayout extends WidgetGroup{
        float space = Scl.scl(10f), prefWidth, prefHeight;
        Seq<Element> seq = new Seq<>();
        int insertPosition = 0;
        boolean invalidated;
        public Group jumps = new WidgetGroup();
        private Seq<JumpCurve> processedJumps = new Seq<>();
        private IntMap<JumpCurve> reprBefore = new IntMap<>();
        private IntMap<JumpCurve> reprAfter = new IntMap<>();
        public boolean updateJumpHeights = true;

        {
            setTransform(true);
        }

        @Override
        public void layout(){
            invalidated = true;
            float cy = 0;
            seq.clear();

            float totalHeight = getChildren().sumf(e -> e.getPrefHeight() + space);
            if(height != totalHeight || width != Scl.scl(targetWidth)){
                height = prefHeight = totalHeight;
                width = prefWidth = Scl.scl(targetWidth);
                invalidateHierarchy();
            }

            //layout everything normally
            for(int i = 0; i < getChildren().size; i++){
                Element e = getChildren().get(i);

                //ignore the dragged element
                if(dragging == e) continue;

                e.setSize(width, e.getPrefHeight());
                e.setPosition(0, totalHeight - cy, Align.topLeft);
                ((StatementElem)e).updateAddress(i);

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

            if(dynamicJumpHeights){
                if(updateJumpHeights) setJumpHeights();
                updateJumpHeights = false;
            }

            if(parent != null && parent instanceof Table){
                setCullingArea(parent.getCullingArea());
            }
        }

        private void setJumpHeights(){
            SnapshotSeq<Element> jumpsChildren = jumps.getChildren();
            processedJumps.clear();
            reprBefore.clear();
            reprAfter.clear();
            jumpsChildren.each(e -> {
                if(!(e instanceof JumpCurve e2)) return;
                e2.prepareHeight();
                if(e2.jumpUIBegin == invalidJump) return;
                if(e2.flipped){
                    JumpCurve prev = reprAfter.get(e2.jumpUIBegin);
                    if(prev != null && prev.jumpUIEnd >= e2.jumpUIEnd) return;
                    reprAfter.put(e2.jumpUIBegin, e2);
                }else{
                    JumpCurve prev = reprBefore.get(e2.jumpUIEnd);
                    if(prev != null && prev.jumpUIBegin <= e2.jumpUIBegin) return;
                    reprBefore.put(e2.jumpUIEnd, e2);
                }
            });
            processedJumps.add(reprBefore.values().toArray());
            processedJumps.add(reprAfter.values().toArray());
            processedJumps.sort((a, b) -> a.jumpUIBegin - b.jumpUIBegin);

            Seq<JumpCurve> occupiers = tmpOccupiers1;
            Bits occupied = tmpBits1;
            occupiers.clear();
            occupied.clear();
            for(int i = 0; i < processedJumps.size; i++){
                JumpCurve cur = processedJumps.get(i);
                occupiers.retainAll(e -> {
                    if(e.jumpUIEnd > cur.jumpUIBegin) return true;
                    occupied.clear(e.predHeight);
                    return false;
                });
                int h = getJumpHeight(i, occupiers, occupied);
                occupiers.add(cur);
                occupied.set(h);
            }

            occupiers.clear();

            jumpsChildren.each(e -> {
                if(!(e instanceof JumpCurve e2)) return;
                if(e2.jumpUIBegin == invalidJump) return;
                e2.predHeight = e2.flipped ? reprAfter.get(e2.jumpUIBegin).predHeight : reprBefore.get(e2.jumpUIEnd).predHeight;
                e2.markedDone = true;
            });
        }

        private int getJumpHeight(int index, Seq<JumpCurve> occupiers, Bits occupied){
            JumpCurve jmp = processedJumps.get(index);
            if(jmp.markedDone) return jmp.predHeight;

            Seq<JumpCurve> tmpOccupiers = tmpOccupiers2;
            Bits tmpOccupied = tmpBits2;
            tmpOccupiers.set(occupiers);
            tmpOccupied.set(occupied);

            int max = -1;
            for(int i = index + 1; i < processedJumps.size; i++){
                JumpCurve cur = processedJumps.get(i);
                if(cur.jumpUIEnd > jmp.jumpUIEnd) continue;
                tmpOccupiers.retainAll(e -> {
                    if(e.jumpUIEnd > cur.jumpUIBegin) return true;
                    tmpOccupied.clear(e.predHeight);
                    return false;
                });
                int h = getJumpHeight(i, tmpOccupiers, tmpOccupied);
                tmpOccupiers.add(cur);
                tmpOccupied.set(h);
                max = Math.max(max, h);
            }

            jmp.predHeight = occupied.nextClearBit(max + 1);
            jmp.markedDone = true;

            tmpOccupiers.clear();

            return jmp.predHeight;
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

            if(invalidated){
                children.each(c -> c.cullable = false);
            }

            super.draw();

            if(invalidated){
                children.each(c -> c.cullable = true);
                invalidated = false;
            }
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
                invalidateHierarchy();
            }

            updateJumpHeights = true;
        }
    }

    public class StatementElem extends Table{
        public LStatement st;
        public int index;
        Label addressLabel;

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

                t.add(st.name()).style(Styles.outlineLabel).name("statement-name").color(color).padRight(8);
                t.add().growX();

                addressLabel = t.add(index + "").style(Styles.outlineLabel).color(color).padRight(8).get();

                //taken from foo's client
                t.button(Icon.add, Styles.logici, () -> Vars.ui.logic.showAddDialog(index + 1))
                .disabled(b -> canvas.statements.getChildren().size >= LExecutor.maxInstructions).size(24f).padRight(6);

                t.button(Icon.copy, Styles.logici, () -> {
                }).size(24f).padRight(6).get().tapped(this::copy);

                t.button(Icon.cancel, Styles.logici, () -> {
                    remove();
                    dragging = null;
                    statements.updateJumpHeights = true;
                }).size(24f);

                t.addListener(new InputListener(){
                    float lastx, lasty;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        //don't start dragging when pressing the menu buttons
                        if(event.targetActor instanceof Image) return false;

                        if(button == KeyCode.mouseMiddle){
                            copy();
                            return false;
                        }

                        Vec2 v = localToParentCoordinates(Tmp.v1.set(x, y));
                        lastx = v.x;
                        lasty = v.y;
                        dragging = StatementElem.this;
                        toFront();
                        statements.updateJumpHeights = true;
                        statements.invalidate();
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer){
                        Vec2 v = localToParentCoordinates(Tmp.v1.set(x, y));

                        translation.add(v.x - lastx, v.y - lasty);
                        lastx = v.x;
                        lasty = v.y;

                        statements.invalidate();
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                        statements.finishLayout();
                    }
                });
            }).growX().height(38);

            row();

            table(t -> {
                t.left();
                t.marginLeft(4);
                t.setColor(color);
                st.build(t);
            }).pad(4).padTop(2).left().grow();

            marginBottom(7);
        }

        public void updateAddress(int index){
            this.index = index;
            addressLabel.setText(index + "");
        }

        public void copy(){
            st.saveUI();
            LStatement copy = st.copy();

            if(copy instanceof JumpStatement st && st.destIndex != -1){
                int index = statements.getChildren().indexOf(this);
                if(index != -1 && index < st.destIndex){
                    st.destIndex ++;
                }
            }

            if(copy != null){
                StatementElem s = new StatementElem(copy);

                statements.addChildAfter(StatementElem.this, s);
                copy.elem = s;
                copy.setupUI();
                statements.updateJumpHeights = true;
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
        Color hoverColor = Pal.place;
        Prov<StatementElem> to;
        boolean selecting;
        float mx, my;
        ClickListener listener;

        public JumpCurve curve;
        public StatementElem elem;

        public JumpButton(Prov<StatementElem> getter, Cons<StatementElem> setter, StatementElem elem){
            super(Tex.logicNode, new ImageButtonStyle(){{
                imageUpColor = Color.white;
            }});

            this.elem = elem;
            to = getter;
            addListener(listener = new ClickListener());

            addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode code){
                    selecting = true;
                    setter.get(null);
                    mx = x;
                    my = y;
                    canvas.statements.updateJumpHeights = true;
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
                    StatementElem elem = canvas.hovered;

                    if(elem != null && !isDescendantOf(elem)){
                        setter.get(elem);
                    }else{
                        setter.get(null);
                    }
                    selecting = false;
                    canvas.statements.updateJumpHeights = true;
                }
            });

            update(() -> {
                if(to.get() != null && to.get().parent == null){
                    setter.get(null);
                }

                setColor(listener.isOver() ? hoverColor : Color.white);
                getStyle().imageUpColor = this.color;
            });

            curve = new JumpCurve(this);
        }

        @Override
        protected void setScene(Scene stage){
            super.setScene(stage);

            if(stage == null){
                curve.remove();
            }else{
                canvas.statements.jumps.addChild(curve);
            }
        }
    }

    public static class JumpCurve extends Element{
        public JumpButton button;
        private boolean invertedHeight;

        // for jump prediction; see DragLayout
        public int predHeight = 0;
        public boolean markedDone = false;
        public int jumpUIBegin = 0, jumpUIEnd = 0;
        public boolean flipped = false;

        private float uiHeight = 60f;

        public JumpCurve(JumpButton button){
            this.button = button;
        }

        @Override
        public void setSize(float width, float height){
            if(height < 0){
                y += height;
                height = -height;
                invertedHeight = true;
            }
            super.setSize(width, height);
        }

        @Override
        public void act(float delta){
            super.act(delta);

            //MDTX(WayZer, 2024/8/6) Support Cull
            invertedHeight = false;
            Group desc = canvas.statements.jumps.parent;
            Vec2 t = Tmp.v1.set(button.getWidth() / 2f, button.getHeight() / 2f);
            button.localToAscendantCoordinates(desc, t);
            setPosition(t.x, t.y);
            Element hover = button.to.get() == null && button.selecting ? canvas.hovered : button.to.get();
            if(hover != null){
                t.set(hover.getWidth(), hover.getHeight() / 2f);
                hover.localToAscendantCoordinates(desc, t);
                setSize(t.x - x, t.y - y);
            }else if(button.selecting){
                setSize(button.mx, button.my);
            }else{
                setSize(0, 0);
            }

            if(button.listener.isOver()){
                toFront();
            }
        }

        @Override
        public void draw(){
            if(height == 0) return;
            Vec2 t = Tmp.v1.set(width, !invertedHeight ? height : 0), r = Tmp.v2.set(0, !invertedHeight ? 0 : height);

            Group desc = canvas.pane;
            localToAscendantCoordinates(desc, r);
            localToAscendantCoordinates(desc, t);

            drawCurve(r.x, r.y, t.x, t.y);

            float s = button.getWidth();
            Draw.color(button.color, parentAlpha);
            Tex.logicNode.draw(t.x + s * 0.75f, t.y - s / 2f, -s, s);
            Draw.reset();
        }

        public void drawCurve(float x, float y, float x2, float y2){
            Lines.stroke(Scl.scl(4f), button.color);
            Draw.alpha(parentAlpha);

            // exponential smoothing
            uiHeight = Mathf.lerp(
                Scl.scl(Core.graphics.isPortrait() ? 20f : 40f) + Scl.scl(Core.graphics.isPortrait() ? 8f : 10f) * (float) predHeight,
                uiHeight,
                dynamicJumpHeights ? Mathf.pow(0.9f, Time.delta) : 0
           );

            //trapezoidal jumps
            float dy = (y2 == y ? 0f : y2 > y ? 1f : -1f) * uiHeight * 0.5f;
            //there's absolutely a better way to detect invalid trapezoids, but this probably isn't *that* slow and I don't care to fix it right now
            if(Intersector.intersectSegments(x, y, x + uiHeight, y + dy, x2, y2, x + uiHeight, y2 - dy, Tmp.v3)){
                Lines.beginLine();
                Lines.linePoint(x, y);
                Lines.linePoint(Tmp.v3.x, Tmp.v3.y);
                Lines.linePoint(x2, y2);
                Lines.endLine();
            }else{
                Lines.beginLine();
                Lines.linePoint(x, y);
                Lines.linePoint(x + uiHeight, y + dy);
                Lines.linePoint(x + uiHeight, y2 - dy);
                Lines.linePoint(x2, y2);
                Lines.endLine();
            }
        }

        public void prepareHeight(){
            if(this.button.to.get() == null){
                this.markedDone = true;
                this.predHeight = 0;
                this.flipped = false;
                this.jumpUIBegin = this.jumpUIEnd = invalidJump;
            }else{
                this.markedDone = false;
                int i = this.button.elem.index;
                int j = this.button.to.get().index;
                this.flipped = i >= j;
                this.jumpUIBegin = Math.min(i,j);
                this.jumpUIEnd = Math.max(i,j);
                // height will be recalculated later
            }
        }
    }
}
