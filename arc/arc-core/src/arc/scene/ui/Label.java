package arc.scene.ui;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.font.Font;
import arc.graphics.font.FontCache;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.GlyphLayout;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.Style;
import arc.util.Align;

import static arc.Core.bundle;
import static arc.Core.scene;

/**
 * A text label, with optional word wrapping.
 * <p>
 * The preferred size of the label is determined by the actual text bounds, unless {@link #setWrap(boolean) word wrap} is enabled.
 * @author Nathan Sweet
 */
public class Label extends Element{
    protected static final Color tempColor = new Color();
    protected static final GlyphLayout prefSizeLayout = new GlyphLayout();

    protected final GlyphLayout layout = new GlyphLayout();
    protected final Vec2 prefSize = new Vec2();
    protected final StringBuilder text = new StringBuilder();
    protected LabelStyle style;
    protected FontCache cache;
    protected int labelAlign = Align.left;
    protected int lineAlign = Align.left;

    protected boolean wrap, wrapFit;
    protected float lastPrefHeight;
    protected boolean prefSizeInvalid = true;
    protected float fontScaleX = 1, fontScaleY = 1;
    protected boolean fontScaleChanged = false;
    protected String ellipsis;

    public Label(Prov<CharSequence> sup){
        this("", new LabelStyle(scene.getStyle(LabelStyle.class)));
        setText(sup);

        try{
            setText(sup.get());
        }catch(Exception ignored){
        }
    }

    public Label(CharSequence text){
        this(text, scene.getStyle(LabelStyle.class));
    }

    public Label(CharSequence text, LabelStyle style){
        if(style == null){
            this.text.setLength(0);
            this.text.append(text);
            return;
        }
        setStyle(new LabelStyle(style));
        if(text != null) setText(text);
        if(text != null && text.length() > 0) setSize(getPrefWidth(), getPrefHeight());
    }

    /**
     * Returns the label's style. Modifying the returned style may not have an effect until {@link #setStyle(LabelStyle)} is
     * called.
     */
    public LabelStyle getStyle(){
        return style;
    }

    public void setStyle(LabelStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        if(style.font == null) throw new IllegalArgumentException("Missing LabelStyle font.");
        this.style = style;
        cache = style.font.newFontCache();
        invalidateHierarchy();
    }

    public void setText(Prov<CharSequence> sup){
        update(() -> setText(sup.get()));
    }

    private void setTextInternal(CharSequence newText){
        if(newText == null) newText = "";
        if(textEquals(newText)) return;
        text.setLength(0);
        text.append(newText);
        invalidateHierarchy();
    }

    public boolean textEquals(CharSequence other){
        int length = text.length();
        if(length != other.length()) return false;
        for(int i = 0; i < length; i++)
            if(text.charAt(i) != other.charAt(i)) return false;
        return true;
    }

    public StringBuilder getText(){
        return text;
    }

    /**
     * @param newText May be null, "" will be used.
     * If this text starts with '$' or '@', this label will look in {@link Core#bundle} for matching text.
     */
    public void setText(CharSequence newText){
        if(bundle != null && newText != null && newText.length() > 0 && (newText.charAt(0) == '$' || newText.charAt(0) == '@')){
            String out = newText.toString().substring(1);
            setTextInternal(bundle.get(out, newText.toString()));
        }else{
            setTextInternal(newText);
        }
    }

    @Override
    public void invalidate(){
        super.invalidate();
        prefSizeInvalid = true;
    }

    private void scaleAndComputePrefSize(){
        if(cache == null) return;
        Font font = cache.getFont();
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        if(fontScaleChanged) font.getData().setScale(fontScaleX, fontScaleY);

        computePrefSize();

        if(fontScaleChanged) font.getData().setScale(oldScaleX, oldScaleY);
    }

    private void computePrefSize(){
        prefSizeInvalid = false;
        GlyphLayout prefSizeLayout = Label.prefSizeLayout;
        if(wrap && ellipsis == null){
            float width = getWidth();
            if(style.background != null) width -= style.background.getLeftWidth() + style.background.getRightWidth();
            prefSizeLayout.setText(cache.getFont(), text, Color.white, width, Align.left, true);
        }else
            prefSizeLayout.setText(cache.getFont(), text, 0, text.length(), Color.white, width, lineAlign, wrap, ellipsis);
        prefSize.set(prefSizeLayout.width, prefSizeLayout.height);
    }

    @Override
    public void layout(){
        if(cache == null) return;
        Font font = cache.getFont();
        float oldScaleX = font.getScaleX();
        float oldScaleY = font.getScaleY();
        if(fontScaleChanged) font.getData().setScale(fontScaleX, fontScaleY);

        boolean wrap = this.wrap && ellipsis == null;
        if(wrap){
            float prefHeight = getPrefHeight();
            if(prefHeight != lastPrefHeight){
                lastPrefHeight = prefHeight;
                invalidateHierarchy();
            }
        }

        float width = getWidth(), height = getHeight();
        Drawable background = style.background;
        float x = 0, y = 0;
        if(background != null){
            x = background.getLeftWidth();
            y = background.getBottomHeight();
            width -= background.getLeftWidth() + background.getRightWidth();
            height -= background.getBottomHeight() + background.getTopHeight();
        }

        GlyphLayout layout = this.layout;
        float textWidth, textHeight;
        if(wrap || text.indexOf("\n") != -1){
            // If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
            layout.setText(font, text, 0, text.length(), Color.white, width, lineAlign, wrap, ellipsis);
            textWidth = layout.width;
            textHeight = layout.height;

            if((labelAlign & Align.left) == 0){
                if((labelAlign & Align.right) != 0)
                    x += width - textWidth;
                else
                    x += (width - textWidth) / 2;
            }
        }else{
            textWidth = width;
            textHeight = font.getData().capHeight;
        }

        if((labelAlign & Align.top) != 0){
            y += cache.getFont().isFlipped() ? 0 : height - textHeight;
            y += style.font.getDescent();
        }else if((labelAlign & Align.bottom) != 0){
            y += cache.getFont().isFlipped() ? height - textHeight : 0;
            y -= style.font.getDescent();
        }else{
            y += (height - textHeight) / 2;
        }
        if(!cache.getFont().isFlipped()) y += textHeight;

        layout.setText(font, text, 0, text.length(), Color.white, textWidth, lineAlign, wrap, ellipsis);
        cache.setText(layout, x, y);

        if(fontScaleChanged) font.getData().setScale(oldScaleX, oldScaleY);
    }

    @Override
    public void draw(){
        validate();
        Color color = tempColor.set(this.color);
        color.a *= parentAlpha;
        if(style.background != null){
            Draw.color(color.r, color.g, color.b, color.a);
            style.background.draw(x, y, width, height);
        }
        if(style.fontColor != null) color.mul(style.fontColor);
        cache.tint(color);
        cache.setPosition(x, y);
        cache.draw();
    }

    @Override
    public float getMinWidth(){
        //wrapped labels don't use their 'pref' width as their min width, as that would be the incorrect, unwrapped width
        return wrap ? 0 : super.getMinWidth();
    }

    @Override
    public float getPrefWidth(){
        if(style == null || (wrap && wrapFit)) return 0;
        if(prefSizeInvalid) scaleAndComputePrefSize();
        float width = prefSize.x;
        Drawable background = style.background;
        if(background != null) width += background.getLeftWidth() + background.getRightWidth();
        return width;
    }

    @Override
    public float getPrefHeight(){
        if(style == null) return 0;
        if(prefSizeInvalid) scaleAndComputePrefSize();
        float descentScaleCorrection = 1;
        if(fontScaleChanged) descentScaleCorrection = fontScaleY / style.font.getScaleY();
        float height = prefSize.y - style.font.getDescent() * descentScaleCorrection * 2;
        Drawable background = style.background;
        if(background != null) height += background.getTopHeight() + background.getBottomHeight();
        return height;
    }

    public GlyphLayout getGlyphLayout(){
        return layout;
    }

    public void setWrap(boolean wrap){
        setWrap(wrap, false);
    }

    /**
     * If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds.
     * If true, the text will word wrap using the width of the label. The preferred width of the label will be 0, it is expected
     * that something external will set the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false.
     * <p>
     * When wrap is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label
     * will need to layout twice: once to set the width of the label and a second time to adjust to the label's new preferred
     * height.
     * @param wrapFit if true, labels will have a prefWidth of 0, and need to have an explicit wrap size set. if false, they will occupy all horizontal space unless an explicit size is set.
     */
    public void setWrap(boolean wrap, boolean wrapFit){
        this.wrap = wrap;
        this.wrapFit = wrapFit;
        invalidateHierarchy();
    }

    public int getLabelAlign(){
        return labelAlign;
    }

    public int getLineAlign(){
        return lineAlign;
    }

    /**
     * @param alignment Aligns all the text within the label (default left center) and each line of text horizontally (default
     * left).
     * @see Align
     */
    public void setAlignment(int alignment){
        setAlignment(alignment, alignment);
    }

    /**
     * @param labelAlign Aligns all the text within the label (default left center).
     * @param lineAlign Aligns each line of text horizontally (default left).
     * @see Align
     */
    public void setAlignment(int labelAlign, int lineAlign){
        this.labelAlign = labelAlign;

        if((lineAlign & Align.left) != 0)
            this.lineAlign = Align.left;
        else if((lineAlign & Align.right) != 0)
            this.lineAlign = Align.right;
        else
            this.lineAlign = Align.center;

        invalidate();
    }

    public void setFontScale(float fontScale){
        setFontScale(fontScale, fontScale);
    }

    public void setFontScale(float fontScaleX, float fontScaleY){
        fontScaleChanged = true;
        this.fontScaleX = fontScaleX;
        this.fontScaleY = fontScaleY;
        invalidateHierarchy();
    }

    public float getFontScaleX(){
        return fontScaleX;
    }

    public void setFontScaleX(float fontScaleX){
        setFontScale(fontScaleX, fontScaleY);
    }

    public float getFontScaleY(){
        return fontScaleY;
    }

    public void setFontScaleY(float fontScaleY){
        setFontScale(fontScaleX, fontScaleY);
    }

    /**
     * When non-null the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur
     * when ellipsis is enabled. Default is false.
     */
    public void setEllipsis(String ellipsis){
        this.ellipsis = ellipsis;
    }

    /**
     * When true the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur when
     * ellipsis is true. Default is false.
     */
    public void setEllipsis(boolean ellipsis){
        if(ellipsis)
            this.ellipsis = "...";
        else
            this.ellipsis = null;
    }

    /** Allows subclasses to access the cache. */
    public FontCache getFontCache(){
        return cache;
    }

    public String toString(){
        return super.toString() + ": " + text;
    }

    /**
     * The style for a label, see {@link Label}.
     * @author Nathan Sweet
     */
    public static class LabelStyle extends Style{
        public Font font;
        /** Optional. */
        public Color fontColor;
        /** Optional. */
        public Drawable background;

        public LabelStyle(){
        }

        public LabelStyle(Font font, Color fontColor){
            this.font = font;
            this.fontColor = fontColor;
        }

        public LabelStyle(LabelStyle style){
            this.font = style.font;
            if(style.fontColor != null) fontColor = new Color(style.fontColor);
            background = style.background;
        }
    }
}
