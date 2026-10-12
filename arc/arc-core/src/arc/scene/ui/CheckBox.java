package arc.scene.ui;

import arc.func.*;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.util.*;

import static arc.Core.scene;

/**
 * A checkbox is a button that contains an image indicating the checked or unchecked state and a label.
 * @author Nathan Sweet
 */
public class CheckBox extends TextButton{
    private Image image;
    private Cell imageCell;
    private CheckBoxStyle style;

    public CheckBox(String text){
        this(text, scene.getStyle(CheckBoxStyle.class));
    }

    public CheckBox(String text, @Nullable Boolc listener){
        this(text);
        if(listener != null) changed(() -> listener.get(isChecked()));
    }

    public CheckBox(String text, CheckBoxStyle style){
        super(text, style);
        clearChildren();
        Label label = getLabel();
        imageCell = add(image = new Image(style.checkboxOff, Scaling.stretch));
        add(label).padLeft(4).get().setWrap(false);
        label.setAlignment(Align.left);
        setSize(getPrefWidth(), getPrefHeight());
    }

    /**
     * Returns the checkbox's style. Modifying the returned style may not have an effect until {@link #setStyle(ButtonStyle)} is
     * called.
     */
    @Override
    public CheckBoxStyle getStyle(){
        return style;
    }

    @Override
    public void setStyle(ButtonStyle style){
        if(!(style instanceof CheckBoxStyle)) throw new IllegalArgumentException("style must be a CheckBoxStyle.");
        super.setStyle(style);
        this.style = (CheckBoxStyle)style;
    }

    @Override
    public void draw(){
        Drawable checkbox = null;
        if(isDisabled()){
            if(isChecked && style.checkboxOnDisabled != null)
                checkbox = style.checkboxOnDisabled;
            else
                checkbox = style.checkboxOffDisabled;
        }
        if(checkbox == null){
            if(isChecked && isOver() && style.checkboxOnOver != null)
                checkbox = style.checkboxOnOver;
            else if(isChecked && style.checkboxOn != null)
                checkbox = style.checkboxOn;
            else if(isOver() && style.checkboxOver != null && !isDisabled())
                checkbox = style.checkboxOver;
            else
                checkbox = style.checkboxOff;
        }
        image.setDrawable(checkbox);
        super.draw();
    }

    public Image getImage(){
        return image;
    }

    public Cell getImageCell(){
        return imageCell;
    }

    /**
     * The style for a select box, see {@link CheckBox}.
     * @author Nathan Sweet
     */
    public static class CheckBoxStyle extends TextButtonStyle{
        public Drawable checkboxOn, checkboxOff;
        /** Optional. */
        public Drawable checkboxOver, checkboxOnDisabled, checkboxOffDisabled, checkboxOnOver;
    }
}
