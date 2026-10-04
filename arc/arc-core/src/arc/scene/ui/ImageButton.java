package arc.scene.ui;

import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Cell;
import arc.util.Scaling;

import static arc.Core.scene;

/**
 * A button with a child {@link Image} to display an image. This is useful when the button must be larger than the image and the
 * image centered on the button. If the image is the size of the button, a {@link Button} without any children can be used, where
 * the {@link Button.ButtonStyle#up}, {@link Button.ButtonStyle#down}, and {@link Button.ButtonStyle#checked} nine patches define
 * the image.
 * @author Nathan Sweet
 */
public class ImageButton extends Button{
    private final Image image;
    private ImageButtonStyle style;

    public ImageButton(){
        this(scene.getStyle(ImageButtonStyle.class));
    }

    public ImageButton(Drawable icon, ImageButtonStyle stylen){
        this(stylen);
        ImageButtonStyle style = new ImageButtonStyle(stylen);
        style.imageUp = icon;

        setStyle(style);
    }

    public ImageButton(TextureRegion region){
        this(scene.getStyle(ImageButtonStyle.class));
        ImageButtonStyle style = new ImageButtonStyle(scene.getStyle(ImageButtonStyle.class));
        style.imageUp = new TextureRegionDrawable(region);

        setStyle(style);
    }

    public ImageButton(TextureRegion region, ImageButtonStyle stylen){
        this(stylen);
        ImageButtonStyle style = new ImageButtonStyle(stylen);
        style.imageUp = new TextureRegionDrawable(region);

        setStyle(style);
    }

    public ImageButton(ImageButtonStyle style){
        super(style);
        image = new Image();
        image.setScaling(Scaling.fit);
        add(image);
        setStyle(style);
        setSize(getPrefWidth(), getPrefHeight());
    }

    public ImageButton(Drawable imageUp){
        this(new ImageButtonStyle(null, null, null, imageUp, null, null));

        ImageButtonStyle style = new ImageButtonStyle(scene.getStyle(ImageButtonStyle.class));
        style.imageUp = imageUp;
        setStyle(style);
    }

    public ImageButton(Drawable imageUp, Drawable imageDown){
        this(new ImageButtonStyle(null, null, null, imageUp, imageDown, null));
    }

    public ImageButton(Drawable imageUp, Drawable imageDown, Drawable imageChecked){
        this(new ImageButtonStyle(null, null, null, imageUp, imageDown, imageChecked));
    }

    @Override
    public ImageButtonStyle getStyle(){
        return style;
    }

    @Override
    public void setStyle(ButtonStyle style){
        if(!(style instanceof ImageButtonStyle))
            throw new IllegalArgumentException("style must be an ImageButtonStyle.");
        super.setStyle(style);
        this.style = (ImageButtonStyle)style;
        if(image != null) updateImage();
    }

    public void replaceImage(Element element){
        getImageCell().setElement(element);
        addChild(element);
        image.remove();
    }

    /** Updates the Image with the appropriate Drawable from the style before it is drawn. */
    protected void updateImage(){
        Drawable drawable = null;
        if(isDisabled() && style.imageDisabled != null)
            drawable = style.imageDisabled;
        else if(isPressed() && style.imageDown != null)
            drawable = style.imageDown;
        else if(isChecked && style.imageChecked != null)
            drawable = (style.imageCheckedOver != null && isOver()) ? style.imageCheckedOver : style.imageChecked;
        else if(isOver() && style.imageOver != null)
            drawable = style.imageOver;
        else if(style.imageUp != null)
            drawable = style.imageUp;

        Color color = image.color;

        if(isDisabled && style.imageDisabledColor != null)
            color = style.imageDisabledColor;
        else if(isPressed() && style.imageDownColor != null)
            color = style.imageDownColor;
        else if(isChecked() && style.imageCheckedColor != null)
            color = style.imageCheckedColor;
        else if(isOver() && style.imageOverColor != null)
            color = style.imageOverColor;
        else if(style.imageUpColor != null)
            color = style.imageUpColor;

        image.setDrawable(drawable);
        image.setColor(color);
    }

    @Override
    public void draw(){
        updateImage();
        super.draw();
    }

    public Image getImage(){
        return image;
    }

    public Cell getImageCell(){
        return getCell(image) == null ? getCells().first() : getCell(image);
    }

    public void resizeImage(float size){
        getImageCell().size(size);
    }

    /**
     * The style for an image button, see {@link ImageButton}.
     * @author Nathan Sweet
     */
    public static class ImageButtonStyle extends ButtonStyle{
        /** Optional. */
        public Drawable imageUp, imageDown, imageOver, imageChecked, imageCheckedOver, imageDisabled;
        public Color imageUpColor, imageCheckedColor, imageDownColor, imageOverColor, imageDisabledColor;

        public ImageButtonStyle(){
        }

        public ImageButtonStyle(Drawable up, Drawable down, Drawable checked, Drawable imageUp, Drawable imageDown,
                                Drawable imageChecked){
            super(up, down, checked);
            this.imageUp = imageUp;
            this.imageDown = imageDown;
            this.imageChecked = imageChecked;
        }

        public ImageButtonStyle(ImageButtonStyle style){
            super(style);
            this.imageUp = style.imageUp;
            this.imageDown = style.imageDown;
            this.imageOver = style.imageOver;
            this.imageChecked = style.imageChecked;
            this.imageCheckedOver = style.imageCheckedOver;
            this.imageDisabled = style.imageDisabled;
            this.imageUpColor = style.imageUpColor;
            this.imageDownColor = style.imageDownColor;
            this.imageOverColor = style.imageOverColor;
            this.imageCheckedColor = style.imageCheckedColor;
            this.imageDisabledColor = style.imageDisabledColor;
        }
    }
}
