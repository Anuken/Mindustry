package arc.scene.ui;

import arc.Core;
import arc.func.Floatc;
import arc.graphics.g2d.NinePatch;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.scene.Element;
import arc.scene.event.ChangeListener.ChangeEvent;
import arc.scene.event.HandCursorListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.style.Drawable;
import arc.util.pooling.Pools;

import static arc.Core.scene;

/**
 * A slider is a horizontal indicator that allows a user to set a value. The slider has a range (min, max) and a stepping between
 * each value the slider represents.
 * <p>
 * {@link ChangeEvent} is fired when the slider knob is moved. Canceling the event will move the knob to where it was previously.
 * <p>
 * For a horizontal progress bar, its preferred height is determined by the larger of the knob and background, and the preferred width
 * is 140, a relatively arbitrary size. These parameters are reversed for a vertical progress bar.
 * @author mzechner
 * @author Nathan Sweet
 */
public class Slider extends ProgressBar{
    int draggingPointer = -1;
    boolean mouseOver;
    private Interp visualInterpolationInverse = Interp.linear;
    private float[] snapValues;
    private float threshold;

    public Slider(float min, float max, float stepSize, boolean vertical){
        this(min, max, stepSize, vertical, scene.getStyle(SliderStyle.class));
    }

    /**
     * Creates a new slider. If horizontal, its width is determined by the prefWidth parameter, its height is determined by the maximum of
     * the height of either the slider {@link NinePatch} or slider handle {@link TextureRegion}. The min and max values determine
     * the range the values of this slider can take on, the stepSize parameter specifies the distance between individual values.
     * E.g. min could be 4, max could be 10 and stepSize could be 0.2, giving you a total of 30 values, 4.0 4.2, 4.4 and so on.
     * @param min the minimum value
     * @param max the maximum value
     * @param stepSize the step size between values
     * @param style the {@link SliderStyle}
     */
    public Slider(float min, float max, float stepSize, boolean vertical, SliderStyle style){
        super(min, max, stepSize, vertical, style);

        addListener(new HandCursorListener());
        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(disabled) return false;
                if(draggingPointer != -1) return false;
                draggingPointer = pointer;
                calculatePositionAndValue(x, y);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(pointer != draggingPointer) return;
                draggingPointer = -1;
                if(!calculatePositionAndValue(x, y)){
                    // Fire an event on touchUp even if the value didn't change, so listeners can see when a drag ends via isDragging.
                    ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
                    fire(changeEvent);
                    Pools.free(changeEvent);
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                calculatePositionAndValue(x, y);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                if(pointer == -1) mouseOver = true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                if(pointer == -1) mouseOver = false;
            }
        });
    }

    /**
     * Returns the slider's style. Modifying the returned style may not have an effect until {@link #setStyle(SliderStyle)} is
     * called.
     */
    @Override
    public SliderStyle getStyle(){
        return (SliderStyle)super.getStyle();
    }

    public void setStyle(SliderStyle style){
        if(style == null) throw new NullPointerException("style cannot be null");
        if(!(style instanceof SliderStyle)) throw new IllegalArgumentException("style must be a SliderStyle.");
        super.setStyle(style);
    }

    @Override
    protected Drawable getKnobDrawable(){
        SliderStyle style = getStyle();
        return (disabled && style.disabledKnob != null) ? style.disabledKnob
        : (isDragging() && style.knobDown != null) ? style.knobDown
        : ((mouseOver && style.knobOver != null) ? style.knobOver : style.knob);
    }

    private float valueAt(float position, float span, float min, float max){
        if(!(span > 0)) return min;
        return min + (max - min) * visualInterpolationInverse.apply(position / span);
    }

    boolean calculatePositionAndValue(float x, float y){
        if(!Float.isFinite(x) || !Float.isFinite(y)) return false;

        final SliderStyle style = getStyle();
        final Drawable knob = getKnobDrawable();
        final Drawable bg = (disabled && style.disabledBackground != null) ? style.disabledBackground : style.background;

        float value;
        float oldPosition = position;
        float span;

        final float min = getMinValue();
        final float max = getMaxValue();

        if(vertical){
            float height = getHeight() - bg.getTopHeight() - bg.getBottomHeight();
            float knobHeight = knob == null ? 0 : knob.getMinHeight();
            span = height - knobHeight;
            position = y - bg.getBottomHeight() - knobHeight * 0.5f;
        }else{
            float width = getWidth() - bg.getLeftWidth() - bg.getRightWidth();
            float knobWidth = knob == null ? 0 : knob.getMinWidth();
            span = width - knobWidth;
            position = x - bg.getLeftWidth() - knobWidth * 0.5f;
        }

        value = valueAt(position, span, min, max);

        if(!Float.isFinite(value) || !Float.isFinite(position)){
            position = oldPosition;
            return false;
        }

        position = Math.max(0, position);
        position = Math.min(span, position);

        float oldValue = value;
        if(!Core.input.keyDown(KeyCode.shiftLeft) && !Core.input.keyDown(KeyCode.shiftRight))
            value = snap(value);
        boolean valueSet = setValue(value);
        if(value == oldValue) position = oldPosition;
        return valueSet;
    }

    public void moved(Floatc listener){
        changed(() -> listener.get(getValue()));
    }

    /** Returns a snapped value. */
    protected float snap(float value){
        if(snapValues == null) return value;
        for(int i = 0; i < snapValues.length; i++){
            if(Math.abs(value - snapValues[i]) <= threshold) return snapValues[i];
        }
        return value;
    }

    /**
     * Will make this progress bar snap to the specified values, if the knob is within the threshold.
     * @param values May be null.
     */
    public void setSnapToValues(float[] values, float threshold){
        this.snapValues = values;
        this.threshold = threshold;
    }

    /** Returns true if the slider is being dragged. */
    public boolean isDragging(){
        return draggingPointer != -1;
    }

    /**
     * Sets the inverse interpolation to use for display. This should perform the inverse of the
     * {@link #setVisualInterpolation(Interp) visual interpolation}.
     */
    public void setVisualInterpolationInverse(Interp interpolation){
        this.visualInterpolationInverse = interpolation;
    }

    /**
     * The style for a slider, see {@link Slider}.
     * @author mzechner
     * @author Nathan Sweet
     */
    public static class SliderStyle extends ProgressBarStyle{
        /** Optional. */
        public Drawable knobOver, knobDown;
    }
}