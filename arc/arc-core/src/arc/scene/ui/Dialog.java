package arc.scene.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.font.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;

import static arc.Core.*;

/**
 * A table that can be dragged and act as a modal window. The top padding is used as the window's title height.
 * <p>
 * The preferred size of a window is the preferred size of the title text and the children as laid out by the table. After adding
 * children to the window, it can be convenient to call {@link #pack()} to size the window to the size of the children.
 * @author Nathan Sweet
 */
public class Dialog extends Table{
    private static Prov<Action>
    defaultShowAction = () -> Actions.sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Interp.fade)),
    defaultHideAction = () -> Actions.fadeOut(0.4f, Interp.fade);
    protected InputListener ignoreTouchDown = new InputListener(){
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
            event.cancel();
            return false;
        }
    };

    private static final Vec2 tmpPosition = new Vec2();
    private static final Vec2 tmpSize = new Vec2();
    private static final int MOVE = 1 << 5;

    protected int edge;
    protected boolean dragging;
    boolean isMovable = false, isModal = true, isResizable = false, center = true;
    int resizeBorder = 8;
    boolean keepWithinStage = true;

    private DialogStyle style;
    private float lastWidth = -1f, lastHeight = -1f;

    Element previousKeyboardFocus, previousScrollFocus;
    FocusListener focusListener;

    public final Table cont, buttons;
    public final Label title;
    public final Table titleTable;

    public Dialog(){
        this("");
    }

    public Dialog(String title){
        this(title, scene.getStyle(DialogStyle.class));
    }

    public Dialog(String title, DialogStyle style){
        if(title == null) throw new IllegalArgumentException("title cannot be null.");
        this.touchable = Touchable.enabled;
        setClip(true);

        this.title = new Label(title, new LabelStyle(style.titleFont, style.titleFontColor));
        this.title.setEllipsis(true);

        titleTable = new Table();
        titleTable.add(this.title).expandX().fillX().minWidth(0);
        add(titleTable).growX().row();

        setStyle(style);
        setWidth(150);
        setHeight(150);

        addCaptureListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                toFront();
                return false;
            }
        });
        addListener(new InputListener(){
            float startX, startY, lastX, lastY;

            private void updateEdge(float x, float y){
                float border = resizeBorder / 2f;
                float width = getWidth(), height = getHeight();
                float padTop = getMarginTop(), padRight = getMarginRight();
                float right = width - padRight;
                edge = 0;
                if(isResizable && x >= getMarginLeft() - border && x <= right + border && y >= getMarginBottom() - border){
                    if(x < getMarginLeft() + border) edge |= Align.left;
                    if(x > right - border) edge |= Align.right;
                    if(y < getMarginBottom() + border) edge |= Align.bottom;
                    if(edge != 0) border += 25;
                    if(x < getMarginLeft() + border) edge |= Align.left;
                    if(x > right - border) edge |= Align.right;
                    if(y < getMarginBottom() + border) edge |= Align.bottom;
                }
                if(isMovable && edge == 0 && y <= height && y >= height - padTop && x >= getMarginLeft() && x <= right)
                    edge = MOVE;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(button == KeyCode.mouseLeft){
                    updateEdge(x, y);
                    dragging = edge != 0;
                    startX = x;
                    startY = y;
                    lastX = x - getWidth();
                    lastY = y - getHeight();
                }
                return edge != 0 || isModal;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                dragging = false;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(!dragging) return;
                float width = getWidth(), height = getHeight();
                float windowX = Dialog.this.x, windowY = Dialog.this.y;

                float minWidth = getMinWidth();
                float minHeight = getMinHeight();
                Scene stage = getScene();
                boolean clampPosition = keepWithinStage && parent == stage.root;

                if((edge & MOVE) != 0){
                    float amountX = x - startX, amountY = y - startY;
                    windowX += amountX;
                    windowY += amountY;
                }
                if((edge & Align.left) != 0){
                    float amountX = x - startX;
                    if(width - amountX < minWidth) amountX = -(minWidth - width);
                    if(clampPosition && windowX + amountX < 0) amountX = -windowX;
                    width -= amountX;
                    windowX += amountX;
                }
                if((edge & Align.bottom) != 0){
                    float amountY = y - startY;
                    if(height - amountY < minHeight) amountY = -(minHeight - height);
                    if(clampPosition && windowY + amountY < 0) amountY = -windowY;
                    height -= amountY;
                    windowY += amountY;
                }
                if((edge & Align.right) != 0){
                    float amountX = x - lastX - width;
                    if(width + amountX < minWidth) amountX = minWidth - width;
                    if(clampPosition && windowX + width + amountX > stage.getWidth())
                        amountX = stage.getWidth() - windowX - width;
                    width += amountX;
                }
                if((edge & Align.top) != 0){
                    float amountY = y - lastY - height;
                    if(height + amountY < minHeight) amountY = minHeight - height;
                    if(clampPosition && windowY + height + amountY > stage.getHeight())
                        amountY = stage.getHeight() - windowY - height;
                    height += amountY;
                }
                setBounds(Math.round(windowX), Math.round(windowY), Math.round(width), Math.round(height));
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                updateEdge(x, y);
                return isModal;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                return isModal;
            }

            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode){
                return isModal;
            }

            @Override
            public boolean keyUp(InputEvent event, KeyCode keycode){
                return isModal;
            }

            @Override
            public boolean keyTyped(InputEvent event, char character){
                return isModal;
            }
        });

        setOrigin(Align.center);

        defaults().pad(3);
        add(cont = new Table()).expand().fill();
        row();
        add(buttons = new Table()).fillX();

        cont.defaults().pad(3);
        buttons.defaults().pad(3);

        focusListener = new FocusListener(){
            @Override
            public void keyboardFocusChanged(FocusEvent event, Element actor, boolean focused){
                if(!focused) focusChanged(event);
            }

            @Override
            public void scrollFocusChanged(FocusEvent event, Element actor, boolean focused){
                if(!focused) focusChanged(event);
            }

            private void focusChanged(FocusEvent event){
                Scene stage = getScene();
                if(isModal && stage != null && stage.root.getChildren().size > 0
                && stage.root.getChildren().peek() == Dialog.this){ // Dialog is top most actor.
                    Element newFocusedActor = event.relatedActor;
                    if(newFocusedActor != null && !newFocusedActor.isDescendantOf(Dialog.this) &&
                    !(newFocusedActor.equals(previousKeyboardFocus) || newFocusedActor.equals(previousScrollFocus)))
                        event.cancel();
                }
            }
        };

        shown(this::updateScrollFocus);
    }

    /**
     * Returns the window's style. Modifying the returned style may not have an effect until {@link #setStyle(DialogStyle)} is
     * called.
     */
    public DialogStyle getStyle(){
        return style;
    }

    public void setStyle(DialogStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        setBackground(style.background);
        //title.setStyle(new LabelStyle(style.titleFont, style.titleFontColor));
        invalidateHierarchy();
    }

    void keepWithinStage(){
        if(!keepWithinStage) return;
        keepInStage();
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(getScene() != null){
            keepWithinStage();
            if(center && !isMovable && this.getActions().size == 0){
                centerWindow();
            }

            //fire resize events.
            if(lastWidth >= 0 && lastHeight >= 0){
                if(!Mathf.equal(lastWidth, scene.root.getWidth()) || !Mathf.equal(lastHeight, scene.root.getHeight())){
                    SceneResizeEvent e = Pools.obtain(SceneResizeEvent.class, SceneResizeEvent::new);
                    fire(e);
                    Pools.free(e);
                }
            }

            lastWidth = scene.root.getWidth();
            lastHeight = scene.root.getHeight();
        }
    }

    @Override
    public void draw(){
        Scene stage = getScene();

        if(stage.getKeyboardFocus() == null){
            //get top dialog in the scene and focus keyboard on that
            int highestDialog = -1;
            Seq<Element> children = scene.root.getChildren();
            for(int i = children.size - 1; i >= 0; i--){
                if(children.get(i) instanceof Dialog){
                    highestDialog = i;
                    break;
                }
            }
            stage.setKeyboardFocus(highestDialog == -1 ? this : children.get(highestDialog));
        }

        if(style.stageBackground != null){
            stageToLocalCoordinates(tmpPosition.set(translation.x, translation.y));
            stageToLocalCoordinates(tmpSize.set(stage.getWidth(), stage.getHeight()));
            drawStageBackground(x + tmpPosition.x, y + tmpPosition.y, x + tmpSize.x, y + tmpSize.y);
        }

        super.draw();
    }

    protected void drawStageBackground(float x, float y, float width, float height){
        Color color = this.color;
        Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
        style.stageBackground.draw(x, y, width, height);
    }

    @Override
    public Element hit(float x, float y, boolean touchable){
        Element hit = super.hit(x, y, touchable);
        if(hit == null && isModal && (!touchable || this.touchable == Touchable.enabled)) return this;
        return hit;
    }

    /** Centers the dialog in the scene. */
    public void centerWindow(){
        setPosition(Math.round(((Core.scene.getWidth() - scene.marginLeft - scene.marginRight) - getWidth()) / 2), Math.round(((Core.scene.getHeight() - scene.marginTop - scene.marginBottom) - getHeight()) / 2));
    }

    public boolean isMovable(){
        return isMovable;
    }

    public void setMovable(boolean isMovable){
        this.isMovable = isMovable;
    }

    public boolean isModal(){
        return isModal;
    }

    public void setModal(boolean isModal){
        this.isModal = isModal;
    }

    public void setKeepWithinStage(boolean keepWithinStage){
        this.keepWithinStage = keepWithinStage;
    }

    public boolean isCentered(){
        return center;
    }

    public void setCentered(boolean center){
        this.center = center;
    }

    public boolean isResizable(){
        return isResizable;
    }

    public void setResizable(boolean isResizable){
        this.isResizable = isResizable;
    }

    public void setResizeBorder(int resizeBorder){
        this.resizeBorder = resizeBorder;
    }

    public boolean isDragging(){
        return dragging;
    }

    public void updateScrollFocus(){
        boolean[] done = {false};

        Core.app.post(() -> forEach(child -> {
            if(done[0]) return;

            if(child instanceof ScrollPane){
                Core.scene.setScrollFocus(child);
                done[0] = true;
            }
        }));
    }

    public static void setHideAction(Prov<Action> prov){
        defaultHideAction = prov;
    }

    public static void setShowAction(Prov<Action> prov){
        defaultShowAction = prov;
    }

    @Override
    protected void setScene(Scene stage){
        if(stage == null)
            addListener(focusListener);
        else
            removeListener(focusListener);
        super.setScene(stage);
    }

    /** Adds a show() listener. */
    public void shown(Runnable run){
        addListener(new VisibilityListener(){
            @Override
            public boolean shown(){
                run.run();
                return false;
            }
        });
    }

    /** Adds a hide() listener. */
    public void hidden(Runnable run){
        addListener(new VisibilityListener(){
            @Override
            public boolean hidden(){
                run.run();
                return false;
            }
        });
    }

    /** Runs the callback when this dialog is resized or hidden. */
    public void resizedShown(Runnable run){
        resized(run);
        shown(run);
    }

    /** Adds a scene resize listener. */
    public void resized(Runnable run){
        resized(false, run);
    }

    /** Adds a scene resize listener, optionally invoking it immediately. */
    public void resized(boolean invoke, Runnable run){
        if(invoke){
            run.run();
        }
        addListener(new ResizeListener(){
            @Override
            public void resized(){
                run.run();
                //refocus scrollpanes automatically after a rebuild
                updateScrollFocus();
            }
        });
    }

    public void addCloseButton(){
        //no default implementation; should be implemented by subclasses
    }

    /** Adds a listener for back/escape keys to hide this dialog. */
    public void closeOnBack(){
        closeOnBack(() -> {});
    }

    public void closeOnBack(Runnable callback){
        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back){
                Core.app.post(this::hide);
                callback.run();
            }
        });
    }

    public boolean isShown(){
        return getScene() != null;
    }

    /** {@link #pack() Packs} the dialog and adds it to the stage with custom action which can be null for instant show */
    public Dialog show(Scene stage, Action action){
        setOrigin(Align.center);
        setClip(false);
        setTransform(true);

        this.fire(new VisibilityEvent(false));

        clearActions();
        removeCaptureListener(ignoreTouchDown);

        previousKeyboardFocus = null;
        Element actor = stage.getKeyboardFocus();
        if(actor != null && !actor.isDescendantOf(this)) previousKeyboardFocus = actor;

        previousScrollFocus = null;
        actor = stage.getScrollFocus();
        if(actor != null && !actor.isDescendantOf(this)) previousScrollFocus = actor;

        pack();
        stage.add(this);
        stage.setKeyboardFocus(this);
        stage.setScrollFocus(this);

        if(action != null) addAction(action);
        pack();

        return this;
    }

    /** Shows this dialog if it was hidden, and vice versa. */
    public void toggle(){
        if(isShown()){
            hide();
        }else{
            show();
        }
    }

    public Dialog show(){
        return show(Core.scene);
    }

    /** {@link #pack() Packs} the dialog and adds it to the stage, centered with default fadeIn action */
    public Dialog show(Scene stage){
        show(stage, defaultShowAction.get());
        centerWindow();
        return this;
    }

    /** Hides the dialog with the given action and then removes it from the stage. */
    public void hide(Action action){
        this.fire(new VisibilityEvent(true));

        Scene stage = getScene();
        if(stage != null){
            removeListener(focusListener);
            if(previousKeyboardFocus != null && (previousKeyboardFocus.getScene() == null || !previousKeyboardFocus.visible)) previousKeyboardFocus = null;
            Element actor = stage.getKeyboardFocus();
            if(actor == null || actor.isDescendantOf(this)) stage.setKeyboardFocus(previousKeyboardFocus);

            if(previousScrollFocus != null && (previousScrollFocus.getScene() == null || !previousScrollFocus.visible)) previousScrollFocus = null;
            actor = stage.getScrollFocus();
            if(actor == null || actor.isDescendantOf(this)) stage.setScrollFocus(previousScrollFocus);
        }
        if(action != null){
            addCaptureListener(ignoreTouchDown);
            addAction(Actions.sequence(action, Actions.removeListener(ignoreTouchDown, true), Actions.remove()));
        }else
            remove();
    }

    /**
     * Hides the dialog. Called automatically when a button is clicked. The default implementation fades out the dialog over 400
     * milliseconds.
     */
    public void hide(){
        if(!isShown()) return;
        setOrigin(Align.center);
        setClip(false);
        setTransform(true);

        hide(defaultHideAction.get());
    }

    public static class DialogStyle extends Style{
        /** Optional. */
        public Drawable background;
        public Font titleFont;
        /** Optional. */
        public Color titleFontColor = new Color(1, 1, 1, 1);
        /** Optional. */
        public Drawable stageBackground;
    }
}
