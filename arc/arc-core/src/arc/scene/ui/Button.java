package arc.scene.ui;

import arc.*;
import arc.struct.*;
import arc.func.Boolp;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.util.pooling.*;

import static arc.Core.*;

/**
 * A button is a {@link Table} with a checked state and additional {@link ButtonStyle style} fields for pressed, unpressed, and
 * checked. Each time a button is clicked, the checked state is toggled. Being a table, a button can contain any other actors.<br>
 * <br>
 * The button's padding is set to the background drawable's padding when the background changes, overwriting any padding set
 * manually. Padding can still be set on the button's table cells.
 * <p>
 * {@link ChangeEvent} is fired when the button is clicked. Cancelling the event will restore the checked button state to what is
 * was previously.
 * <p>
 * The preferred size of the button is determined by the background and the button contents.
 * @author Nathan Sweet
 */
public class Button extends Table implements Disableable{
    boolean isChecked, isDisabled;
    ButtonGroup buttonGroup;
    Boolp disabledProvider;
    private ButtonStyle style;
    private ClickListener clickListener;
    private boolean programmaticChangeEvents;

    public Button(ButtonStyle style){
        initialize();
        setStyle(style);
        setSize(getPrefWidth(), getPrefHeight());
    }

    /** Creates a button without setting the style or size. At least a style must be set before using this button. */
    public Button(){
        initialize();
        this.style = scene.getStyle(ButtonStyle.class);

        Drawable background;
        if(isPressed() && !isDisabled()){
            background = style.down == null ? style.up : style.down;
        }else{
            if(isDisabled() && style.disabled != null)
                background = style.disabled;
            else if(isChecked && style.checked != null)
                background = (isOver() && style.checkedOver != null) ? style.checkedOver : style.checked;
            else if(isOver() && style.over != null)
                background = style.over;
            else
                background = style.up;
        }
        setBackground(background);
    }

    public Button(Drawable up){
        this(new ButtonStyle(up, null, null));
    }

    public Button(Drawable up, Drawable down){
        this(new ButtonStyle(up, down, null));
    }

    public Button(Drawable up, Drawable down, Drawable checked){
        this(new ButtonStyle(up, down, checked));
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(disabledProvider != null){
            setDisabled(disabledProvider.get());
        }
    }

    private void initialize(){
        this.touchable = Touchable.enabled;
        addListener(clickListener = new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                if(isDisabled()) return;
                setChecked(!isChecked, true);
            }
        });
        addListener(new HandCursorListener());
    }

    @SuppressWarnings("unchecked")
    void setChecked(boolean isChecked, boolean fireEvent){
        if(this.isChecked == isChecked) return;
        if(buttonGroup != null && !buttonGroup.canCheck(this, isChecked)) return;
        this.isChecked = isChecked;

        if(fireEvent){
            ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
            if(fire(changeEvent)) this.isChecked = !isChecked;
            Pools.free(changeEvent);
        }
    }

    /**
     * Toggles the checked state. This method changes the checked state, which fires a {@link ChangeEvent} (if programmatic change
     * events are enabled), so can be used to simulate a button click.
     */
    public void toggle(){
        setChecked(!isChecked);
    }

    public boolean isChecked(){
        return isChecked;
    }

    public void setChecked(boolean isChecked){
        setChecked(isChecked, programmaticChangeEvents);
    }

    public boolean isPressed(){
        return clickListener.isVisualPressed();
    }

    public boolean isOver(){
        return clickListener.isOver();
    }

    public ClickListener getClickListener(){
        return clickListener;
    }

    @Override
    public boolean isDisabled(){
        return isDisabled;
    }

    public void setDisabled(Boolp prov){
        this.disabledProvider = prov;
    }

    /** When true, the button will not toggle {@link #isChecked()} when clicked and will not fire a {@link ChangeEvent}. */
    @Override
    public void setDisabled(boolean isDisabled){
        this.isDisabled = isDisabled;
    }

    public boolean childrenPressed(){
        boolean[] b = {false};
        Vec2 v = new Vec2();

        forEach(element -> {
            element.stageToLocalCoordinates(v.set(input.mouseX(), input.mouseY()));
            if(element instanceof Button && (((Button)element).getClickListener().isOver(element, v.x, v.y))){
                b[0] = true;
            }
        });

        return b[0];
    }

    /**
     * If false, {@link #setChecked(boolean)} and {@link #toggle()} will not fire {@link ChangeEvent}, event will be fired only
     * when user clicked the button
     */
    public void setProgrammaticChangeEvents(boolean programmaticChangeEvents){
        this.programmaticChangeEvents = programmaticChangeEvents;
    }

    /**
     * Returns the button's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is
     * called.
     */
    public ButtonStyle getStyle(){
        return style;
    }

    public void setStyle(ButtonStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;

        Drawable background;
        if(isPressed() && !isDisabled()){
            background = style.down == null ? style.up : style.down;
        }else{
            if(isDisabled() && style.disabled != null)
                background = style.disabled;
            else if(isChecked && style.checked != null)
                background = (isOver() && style.checkedOver != null) ? style.checkedOver : style.checked;
            else if(isOver() && style.over != null)
                background = style.over;
            else
                background = style.up;
        }
        setBackground(background);
    }

    /** @return May be null. */
    public ButtonGroup getButtonGroup(){
        return buttonGroup;
    }

    @Override
    public void draw(){
        validate();

        boolean isDisabled = isDisabled();
        boolean isPressed = isPressed();
        boolean isChecked = isChecked();
        boolean isOver = isOver();

        Drawable background = null;
        if(isDisabled && style.disabled != null)
            background = style.disabled;
        else if(isPressed && style.down != null)
            background = style.down;
        else if(isChecked && style.checked != null)
            background = (style.checkedOver != null && isOver) ? style.checkedOver : style.checked;
        else if(isOver && style.over != null){
            background = style.over;
        }else if(style.up != null)
            background = style.up;

        setBackground(background);

        float offsetX, offsetY;
        if(isPressed && !isDisabled){
            offsetX = style.pressedOffsetX;
            offsetY = style.pressedOffsetY;
        }else if(isChecked && !isDisabled){
            offsetX = style.checkedOffsetX;
            offsetY = style.checkedOffsetY;
        }else{
            offsetX = style.unpressedOffsetX;
            offsetY = style.unpressedOffsetY;
        }

        Seq<Element> children = getChildren();
        for(int i = 0; i < children.size; i++)
            children.get(i).moveBy(offsetX, offsetY);
        super.draw();
        for(int i = 0; i < children.size; i++)
            children.get(i).moveBy(-offsetX, -offsetY);

        Scene stage = getScene();
        if(stage != null && stage.getActionsRequestRendering() && isPressed != clickListener.isPressed())
            Core.graphics.requestRendering();
    }

    @Override
    public float getPrefWidth(){
        float width = super.getPrefWidth();
        if(style.up != null) width = Math.max(width, style.up.getMinWidth());
        if(style.down != null) width = Math.max(width, style.down.getMinWidth());
        if(style.checked != null) width = Math.max(width, style.checked.getMinWidth());
        return width;
    }

    @Override
    public float getPrefHeight(){
        float height = super.getPrefHeight();
        if(style.up != null) height = Math.max(height, style.up.getMinHeight());
        if(style.down != null) height = Math.max(height, style.down.getMinHeight());
        if(style.checked != null) height = Math.max(height, style.checked.getMinHeight());
        return height;
    }

    @Override
    public float getMinWidth(){
        return getPrefWidth();
    }

    @Override
    public float getMinHeight(){
        return getPrefHeight();
    }

    /**
     * The style for a button, see {@link Button}.
     * @author mzechner
     */
    public static class ButtonStyle extends Style{
        /** Optional. */
        public Drawable up, down, over, checked, checkedOver, disabled;
        /** Optional. */
        public float pressedOffsetX, pressedOffsetY, unpressedOffsetX,
        unpressedOffsetY, checkedOffsetX, checkedOffsetY;

        public ButtonStyle(){
        }

        public ButtonStyle(Drawable up, Drawable down, Drawable checked){
            this.up = up;
            this.down = down;
            this.checked = checked;
        }

        public ButtonStyle(ButtonStyle style){
            this.up = style.up;
            this.down = style.down;
            this.over = style.over;
            this.checked = style.checked;
            this.checkedOver = style.checkedOver;
            this.disabled = style.disabled;
            this.pressedOffsetX = style.pressedOffsetX;
            this.pressedOffsetY = style.pressedOffsetY;
            this.unpressedOffsetX = style.unpressedOffsetX;
            this.unpressedOffsetY = style.unpressedOffsetY;
            this.checkedOffsetX = style.checkedOffsetX;
            this.checkedOffsetY = style.checkedOffsetY;
        }
    }
}
