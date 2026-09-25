package mindustry.graphics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;

/** A nine-patch glow whose border can be redrawn smaller/larger than the pixels it was baked at; see {@link GlowCorner}. */
public class GlowDrawable extends NinePatchDrawable{
    /** on-screen pixels per baked pixel; 1 draws the border at its native baked size. */
    private float scale = 1f;

    GlowDrawable(NinePatch patch){
        super(patch);
    }

    GlowDrawable(GlowDrawable drawable){
        super(drawable);
        scale = drawable.scale;
    }

    /** @return a copy whose border is redrawn at {@code scale} times its baked size; linear filtering smooths the resample. */
    GlowDrawable scaled(float scale){
        GlowDrawable drawable = new GlowDrawable(this);
        drawable.scale = scale;
        return drawable;
    }

    @Override
    public GlowDrawable tint(Color tint){
        GlowDrawable drawable = new GlowDrawable(this);
        drawable.setPatch(new NinePatch(drawable.getPatch(), tint));
        return drawable;
    }

    @Override
    public void draw(float x, float y, float width, float height){
        CornerGenerator.uploadPending();
        getPatch().draw(x, y, 0, 0, width / scale, height / scale, scale, scale, 0);
    }

    //pad getters aren't scaled by default (setPatch stores the raw baked pad); the glow spreads equally on every side, so all four match
    @Override
    public float getLeftWidth(){
        return getPatch().getPadLeft() * scale;
    }

    @Override
    public float getRightWidth(){
        return getPatch().getPadRight() * scale;
    }

    @Override
    public float getTopHeight(){
        return getPatch().getPadTop() * scale;
    }

    @Override
    public float getBottomHeight(){
        return getPatch().getPadBottom() * scale;
    }
}