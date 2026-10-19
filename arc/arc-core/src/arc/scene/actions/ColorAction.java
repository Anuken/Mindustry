package arc.scene.actions;

import arc.graphics.Color;
import arc.scene.*;

/**
 * Sets the actor's color (or a specified color), from the current to the new color. Note this action transitions from the color
 * at the time the action starts to the specified color.
 * @author Nathan Sweet
 */
public class ColorAction extends TemporalAction{
    private final Color end = new Color();
    private float startR, startG, startB, startA;
    private Color color;

    @Override
    protected void begin(){
        if(color == null) color = target.color;
        startR = color.r;
        startG = color.g;
        startB = color.b;
        startA = color.a;
    }

    @Override
    protected void update(float percent){
        float r = startR + (end.r - startR) * percent;
        float g = startG + (end.g - startG) * percent;
        float b = startB + (end.b - startB) * percent;
        float a = startA + (end.a - startA) * percent;
        color.set(r, g, b, a);
    }

    @Override
    public void reset(){
        super.reset();
        color = null;
    }

    public Color getColor(){
        return color;
    }

    /**
     * Sets the color to modify. If null (the default), the {@link #getActor() actor's} {@link Element#getColor() color} will be
     * used.
     */
    public void setColor(Color color){
        this.color = color;
    }

    public Color getEndColor(){
        return end;
    }

    /** Sets the color to transition to. Required. */
    public void setEndColor(Color color){
        end.set(color);
    }
}
