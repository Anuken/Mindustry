package arc.scene.actions;

import arc.util.Align;

/**
 * Moves an actor from its current position to a specific position.
 * @author Nathan Sweet
 */
public class MoveToAction extends TemporalAction{
    private float startX, startY;
    private float endX, endY;
    private int alignment = Align.bottomLeft;

    @Override
    protected void begin(){
        startX = target.getX(alignment);
        startY = target.getY(alignment);
    }

    @Override
    protected void update(float percent){
        target.setPosition(startX + (endX - startX) * percent, startY + (endY - startY) * percent, alignment);
    }

    @Override
    public void reset(){
        super.reset();
        alignment = Align.bottomLeft;
    }

    public void setPosition(float x, float y){
        endX = x;
        endY = y;
    }

    public void setPosition(float x, float y, int alignment){
        endX = x;
        endY = y;
        this.alignment = alignment;
    }

    public float getX(){
        return endX;
    }

    public void setX(float x){
        endX = x;
    }

    public float getY(){
        return endY;
    }

    public void setY(float y){
        endY = y;
    }

    public int getAlignment(){
        return alignment;
    }

    public void setAlignment(int alignment){
        this.alignment = alignment;
    }
}
