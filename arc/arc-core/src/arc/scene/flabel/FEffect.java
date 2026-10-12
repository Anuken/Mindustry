package arc.scene.flabel;

import arc.math.*;

/** Abstract text effect. */
public abstract class FEffect{
    private static final float fadeoutSplit = 0.25f;

    public int indexStart = -1;
    public int indexEnd = -1;
    public float duration = Float.POSITIVE_INFINITY;
    public String endToken;
    protected float totalTime;

    /** Applies token parameters from the parsed FLabel. Uses semicolons as separators. */
    public void applyParams(String[] params){

    }

    public void update(float delta){
        totalTime += delta;
    }

    /** Applies the effect to the given glyph. */
    public final void apply(FLabel label, FGlyph glyph, int glyphIndex, float delta){
        int localIndex = glyphIndex - indexStart;
        onApply(label, glyph, localIndex, delta);
    }

    /** Called when this effect should be applied to the given glyph. */
    protected abstract void onApply(FLabel label, FGlyph glyph, int localIndex, float delta);

    /** Returns whether or not this effect is finished and should be removed. Note that effects are infinite by default. */
    public boolean isFinished(){
        return totalTime > duration;
    }

    /** Calculates the fadeout of this effect, if any. Only considers the second half of the duration. */
    protected float calculateFadeout(){
        if(Float.isInfinite(duration)) return 1;

        // Calculate raw progress
        float progress = Mathf.clamp(totalTime / duration, 0, 1);

        // If progress is before the split point, return a full factor
        if(progress < fadeoutSplit) return 1;

        // Otherwise calculate from the split point
        return Interp.smooth.apply(1, 0, (progress - fadeoutSplit) / (1f - fadeoutSplit));
    }

    /**
     * Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1 that
     * loops in a ping-pong mode.
     */
    protected float calculateProgress(float modifier){
        return calculateProgress(modifier, 0, true);
    }

    /**
     * Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1 that
     * loops in a ping-pong mode.
     */
    protected float calculateProgress(float modifier, float offset){
        return calculateProgress(modifier, offset, true);
    }

    /** Calculates a linear progress dividing the total time by the given modifier. Returns a value between 0 and 1. */
    protected float calculateProgress(float modifier, float offset, boolean pingpong){
        float progress = totalTime / modifier + offset;
        while(progress < 0.0f){
            progress += 2.0f;
        }
        if(pingpong){
            progress %= 2f;
            if(progress > 1.0f) progress = 1f - (progress - 1f);
        }else{
            progress %= 1.0f;
        }
        progress = Mathf.clamp(progress, 0, 1);
        return progress;
    }

    /** Returns the line height of the label controlling this effect. */
    protected float getLineHeight(FLabel label){
        return label.getFontCache().getFont().getLineHeight() * label.getFontScaleY();
    }

}
