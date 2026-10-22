package arc.scene.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.style.*;
import arc.scene.utils.*;
import arc.util.pooling.*;

/**
 * A progress bar is a widget that visually displays the progress of some activity or a value within given range. The progress
 * bar has a range (min, max) and a stepping between each value it represents. The percentage of completeness typically starts out
 * as an empty progress bar and gradually becomes filled in as the task or variable value progresses.
 * <p>
 * {@link ChangeEvent} is fired when the progress bar knob is moved. Cancelling the event will move the knob to where it was
 * previously.
 * <p>
 * For a horizontal progress bar, its preferred height is determined by the larger of the knob and background, and the preferred width
 * is 140, a relatively arbitrary size. These parameters are reversed for a vertical progress bar.
 * @author mzechner
 * @author Nathan Sweet
 */
public class ProgressBar extends Element implements Disableable{
    final boolean vertical;
    float position;
    boolean disabled;
    private ProgressBarStyle style;
    private float min, max, stepSize;
    private float value, animateFromValue;
    private float animateDuration, animateTime;
    private Interp animateInterpolation = Interp.linear;
    private Interp visualInterpolation = Interp.linear;
    private boolean round = true;

    /**
     * Creates a new progress bar. If horizontal, its width is determined by the prefWidth parameter, and its height is determined by the
     * maximum of the height of either the progress bar {@link NinePatch} or progress bar handle {@link TextureRegion}. The min and
     * max values determine the range the values of this progress bar can take on, the stepSize parameter specifies the distance
     * between individual values.
     * <p>
     * E.g. min could be 4, max could be 10 and stepSize could be 0.2, giving you a total of 30 values, 4.0 4.2, 4.4 and so on.
     * @param min the minimum value
     * @param max the maximum value
     * @param stepSize the step size between values
     * @param style the {@link ProgressBarStyle}
     */
    public ProgressBar(float min, float max, float stepSize, boolean vertical, ProgressBarStyle style){
        if(min > max) throw new IllegalArgumentException("max must be > min. min,max: " + min + ", " + max);
        if(stepSize < 0) throw new IllegalArgumentException("stepSize must be finite and >= 0: " + stepSize);
        setStyle(style);
        this.min = min;
        this.max = max;
        this.stepSize = stepSize;
        this.vertical = vertical;
        this.value = min;
        setSize(getPrefWidth(), getPrefHeight());
    }

    /**
     * Returns the progress bar's style. Modifying the returned style may not have an effect until
     * {@link #setStyle(ProgressBarStyle)} is called.
     */
    public ProgressBarStyle getStyle(){
        return style;
    }

    public void setStyle(ProgressBarStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        invalidateHierarchy();
    }

    @Override
    public void act(float delta){
        super.act(delta);
        if(animateTime > 0){
            animateTime -= delta;
            Scene stage = getScene();
            if(stage != null && stage.getActionsRequestRendering()) Core.graphics.requestRendering();
        }
    }

    @Override
    public void draw(){
        ProgressBarStyle style = this.style;
        boolean disabled = this.disabled;
        final Drawable knob = getKnobDrawable();
        final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;
        final Drawable knobBefore = (disabled && style.disabledKnobBefore != null) ? style.disabledKnobBefore : style.knobBefore;
        final Drawable knobAfter = (disabled && style.disabledKnobAfter != null) ? style.disabledKnobAfter : style.knobAfter;

        Color color = this.color;
        float x = this.x;
        float y = this.y;
        float width = getWidth();
        float height = getHeight();
        float knobHeight = knob == null ? 0 : knob.getMinHeight();
        float knobWidth = knob == null ? 0 : knob.getMinWidth();
        float percent = getVisualPercent();

        Draw.color(color.r, color.g, color.b, color.a * parentAlpha);

        if(vertical){
            float positionHeight = height;

            float bgTopHeight = 0;
            if(bg != null){
                if(round)
                    bg.draw(Math.round(x + (width - bg.getMinWidth()) * 0.5f), y, Math.round(bg.getMinWidth()), height);
                else
                    bg.draw(x + width - bg.getMinWidth() * 0.5f, y, bg.getMinWidth(), height);
                bgTopHeight = bg.getTopHeight();
                positionHeight -= bgTopHeight + bg.getBottomHeight();
            }

            float knobHeightHalf = 0;
            if(min != max){
                if(knob == null){
                    knobHeightHalf = knobBefore == null ? 0 : knobBefore.getMinHeight() * 0.5f;
                    position = (positionHeight - knobHeightHalf) * percent;
                    position = Math.min(positionHeight - knobHeightHalf, position);
                }else{
                    knobHeightHalf = knobHeight * 0.5f;
                    position = (positionHeight - knobHeight) * percent;
                    position = Math.min(positionHeight - knobHeight, position) + bg.getBottomHeight();
                }
                position = Math.max(0, position);
            }

            if(knobBefore != null){
                float offset = 0;
                if(bg != null) offset = bgTopHeight;
                if(round)
                    knobBefore.draw(Math.round(x + (width - knobBefore.getMinWidth()) * 0.5f), Math.round(y + offset), Math.round(knobBefore.getMinWidth()),
                    Math.round(position + knobHeightHalf));
                else
                    knobBefore.draw(x + (width - knobBefore.getMinWidth()) * 0.5f, y + offset, knobBefore.getMinWidth(),
                    position + knobHeightHalf);
            }
            if(knobAfter != null){
                if(round)
                    knobAfter.draw(Math.round(x + (width - knobAfter.getMinWidth()) * 0.5f), Math.round(y + position + knobHeightHalf),
                    Math.round(knobAfter.getMinWidth()), Math.round(height - position - knobHeightHalf));
                else
                    knobAfter.draw(x + (width - knobAfter.getMinWidth()) * 0.5f, y + position + knobHeightHalf,
                    knobAfter.getMinWidth(), height - position - knobHeightHalf);
            }
            if(knob != null){
                if(round)
                    knob.draw(Math.round(x + (width - knobWidth) * 0.5f), Math.round(y + position), Math.round(knobWidth), Math.round(knobHeight));
                else
                    knob.draw(x + (width - knobWidth) * 0.5f, y + position, knobWidth, knobHeight);
            }
        }else{
            float positionWidth = width;
            float bgLeftWidth = 0;

            if(bg != null){
                //currently draws background under *everything*, not limited by bg height
                bg.draw(x, y, width, height);
            }

            float knobWidthHalf = 0;
            if(min != max){
                if(knob == null){
                    knobWidthHalf = knobBefore == null ? 0 : knobBefore.getMinWidth() * 0.5f;
                    position = (positionWidth - knobWidthHalf) * percent;
                    position = Math.min(positionWidth - knobWidthHalf, position);
                }else{
                    knobWidthHalf = knobWidth * 0.5f;
                    position = (positionWidth - knobWidth) * percent;
                    position = Math.min(positionWidth - knobWidth, position) + bgLeftWidth;
                }
                position = Math.max(0, position);
            }

            if(knobBefore != null){
                float offset = 0;
                if(bg != null) offset = bgLeftWidth;
                if(round)
                    knobBefore.draw(Math.round(x + offset), Math.round(y + (height - knobBefore.getMinHeight()) * 0.5f),
                    Math.round(position + knobWidthHalf), Math.round(knobBefore.getMinHeight()));
                else
                    knobBefore.draw(x + offset, y + (height - knobBefore.getMinHeight()) * 0.5f,
                    position + knobWidthHalf, knobBefore.getMinHeight());
            }
            if(knobAfter != null){
                if(round)
                    knobAfter.draw(Math.round(x + position + knobWidthHalf), Math.round(y + (height - knobAfter.getMinHeight()) * 0.5f),
                    Math.round(width - position - knobWidthHalf), Math.round(knobAfter.getMinHeight()));
                else
                    knobAfter.draw(x + position + knobWidthHalf, y + (height - knobAfter.getMinHeight()) * 0.5f,
                    width - position - knobWidthHalf, knobAfter.getMinHeight());
            }
            if(knob != null){
                if(round)
                    knob.draw(Math.round(x + position), Math.round(y + (height - knobHeight) * 0.5f), Math.round(knobWidth), Math.round(knobHeight));
                else
                    knob.draw(x + position, y + (height - knobHeight) * 0.5f, knobWidth, knobHeight);
            }
        }
    }

    public float getValue(){
        return value;
    }

    /** If {@link #setAnimateDuration(float) animating} the progress bar value, this returns the value current displayed. */
    public float getVisualValue(){
        if(animateTime > 0 && animateDuration > 0)
            return animateInterpolation.apply(animateFromValue, value, 1 - animateTime / animateDuration);
        return value;
    }

    public float getPercent(){
        return percentOf(value);
    }

    public float getVisualPercent(){
        return visualInterpolation.apply(percentOf(getVisualValue()));
    }

    private float percentOf(float value){
        double range = (double)max - min;
        if(!(range > 0)) return 0f;
        return (float)(((double)value - min) / range);
    }

    protected Drawable getKnobDrawable(){
        return (disabled && style.disabledKnob != null) ? style.disabledKnob : style.knob;
    }

    /** Returns progress bar visual position within the range. */
    protected float getKnobPosition(){
        return this.position;
    }

    /**
     * Sets the progress bar position, rounded to the nearest step size and clamped to the minimum and maximum values.
     * {@link #clamp(float)} can be overridden to allow values outside of the progress bar's min/max range.
     * @return false if the value was not changed because the progress bar already had the value or it was canceled by a
     * listener.
     */
    public boolean setValue(float value){
        return setValue(value, true);
    }

    /** Sets the value, optionally skipping the changed event. */
    public boolean setValue(float value, boolean fireChanged){
        if(Float.isNaN(value)) return false;
        if(stepSize > 0 && !Float.isInfinite(value)) value = Math.round(value / stepSize) * stepSize;
        value = clamp(value);
        float oldValue = this.value;
        if(value == oldValue) return false;
        float oldVisualValue = getVisualValue();
        this.value = value;
        ChangeEvent changeEvent = fireChanged ? Pools.obtain(ChangeEvent.class, ChangeEvent::new) : null;
        boolean cancelled = fireChanged && fire(changeEvent);
        if(cancelled)
            this.value = oldValue;
        else if(animateDuration > 0){
            animateFromValue = oldVisualValue;
            animateTime = animateDuration;
        }
        if(fireChanged) Pools.free(changeEvent);
        return !cancelled;
    }

    /**
     * Clamps the value to the progress bar's min/max range. This can be overridden to allow a range different from the progress
     * bar knob's range.
     */
    protected float clamp(float value){
        return Mathf.clamp(value, min, max);
    }

    /** Sets the range of this progress bar. The progress bar's current value is clamped to the range. */
    public void setRange(float min, float max){
        if(min > max) throw new IllegalArgumentException("min must be <= max");
        this.min = min;
        this.max = max;
        if(value < min)
            setValue(min);
        else if(value > max) setValue(max);
    }

    @Override
    public float getPrefWidth(){
        if(vertical){
            final Drawable knob = getKnobDrawable();
            final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;
            return Math.max(knob == null ? 0 : knob.getMinWidth(), bg.getMinWidth());
        }else
            return 140;
    }

    @Override
    public float getPrefHeight(){
        if(vertical)
            return 140;
        else{
            final Drawable knob = getKnobDrawable();
            final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;
            return Math.max(knob == null ? 0 : knob.getMinHeight(), bg == null ? 0 : bg.getMinHeight());
        }
    }

    public float getMinValue(){
        return this.min;
    }

    public float getMaxValue(){
        return this.max;
    }

    public float getStepSize(){
        return this.stepSize;
    }

    public void setStepSize(float stepSize){
        if(stepSize < 0) throw new IllegalArgumentException("steps must be finite and >= 0: " + stepSize);
        this.stepSize = stepSize;
    }

    /** If > 0, changes to the progress bar value via {@link #setValue(float)} will happen over this duration in seconds. */
    public void setAnimateDuration(float duration){
        this.animateDuration = duration;
    }

    /** Sets the interpolation to use for {@link #setAnimateDuration(float)}. */
    public void setAnimateInterpolation(Interp animateInterpolation){
        if(animateInterpolation == null) throw new IllegalArgumentException("animateInterpolation cannot be null.");
        this.animateInterpolation = animateInterpolation;
    }

    /** Sets the interpolation to use for display. */
    public void setVisualInterpolation(Interp interpolation){
        this.visualInterpolation = interpolation;
    }

    /** If true (the default), inner Drawable positions and sizes are rounded to integers. */
    public void setRound(boolean round){
        this.round = round;
    }

    @Override
    public boolean isDisabled(){
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled){
        this.disabled = disabled;
    }

    /** True if the progress bar is vertical, false if it is horizontal. **/
    public boolean isVertical(){
        return vertical;
    }

    /**
     * The style for a progress bar, see {@link ProgressBar}.
     * @author mzechner
     * @author Nathan Sweet
     */
    public static class ProgressBarStyle extends Style{
        /** The progress bar background, stretched only in one direction. Optional. */
        public Drawable background;
        /** Optional. **/
        public Drawable disabledBackground;
        /** Optional, centered on the background. */
        public Drawable knob, disabledKnob;
        /** Optional. */
        public Drawable knobBefore, knobAfter, disabledKnobBefore, disabledKnobAfter;

        public ProgressBarStyle(){
        }

        public ProgressBarStyle(Drawable background, Drawable knob){
            this.background = background;
            this.knob = knob;
        }

        public ProgressBarStyle(ProgressBarStyle style){
            this.background = style.background;
            this.disabledBackground = style.disabledBackground;
            this.knob = style.knob;
            this.disabledKnob = style.disabledKnob;
            this.knobBefore = style.knobBefore;
            this.knobAfter = style.knobAfter;
            this.disabledKnobBefore = style.disabledKnobBefore;
            this.disabledKnobAfter = style.disabledKnobAfter;
        }
    }
}