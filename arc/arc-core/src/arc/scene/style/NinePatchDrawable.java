package arc.scene.style;

import arc.graphics.Color;
import arc.graphics.g2d.NinePatch;

/**
 * Drawable for a {@link NinePatch}.
 * <p>
 * The drawable sizes are set when the ninepatch is set, but they are separate values. Eg, {@link Drawable#getLeftWidth()} could
 * be set to more than {@link NinePatch#getLeftWidth()} in order to provide more space on the left than actually exists in the
 * ninepatch.
 * <p>
 * The min size is set to the ninepatch total size by default. It could be set to the left+right and top+bottom, excluding the
 * middle size, to allow the drawable to be sized down as small as possible.
 * @author Nathan Sweet
 */
public class NinePatchDrawable extends BaseDrawable implements TransformDrawable{
    protected NinePatch patch;

    /** Creates an uninitialized NinePatchDrawable. The ninepatch must be {@link #setPatch(NinePatch) set} before use. */
    public NinePatchDrawable(){
    }

    public NinePatchDrawable(NinePatch patch){
        setPatch(patch);
    }

    public NinePatchDrawable(NinePatchDrawable drawable){
        super(drawable);
        setPatch(drawable.patch);
    }

    @Override
    public void draw(float x, float y, float width, float height){
        patch.draw(x, y, width, height);
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation){
        patch.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
    }

    public NinePatch getPatch(){
        return patch;
    }

    public void setPatch(NinePatch patch){
        this.patch = patch;
        setMinWidth(patch.getTotalWidth());
        setMinHeight(patch.getTotalHeight());
        setTopHeight(patch.getPadTop());
        setRightWidth(patch.getPadRight());
        setBottomHeight(patch.getPadBottom());
        setLeftWidth(patch.getPadLeft());
    }

    /** Creates a new drawable that renders the same as this drawable tinted the specified color. */
    public NinePatchDrawable tint(Color tint){
        NinePatchDrawable drawable = new NinePatchDrawable(this);
        drawable.setPatch(new NinePatch(drawable.getPatch(), tint));
        return drawable;
    }

    /** Creates a new drawable that renders the same as this drawable with a top-to-bottom gradient tint. */
    public NinePatchDrawable tint(Color top, Color bottom){
        NinePatchDrawable drawable = new NinePatchDrawable(this);
        drawable.setPatch(new NinePatch(drawable.getPatch(), top, bottom));
        return drawable;
    }
}
