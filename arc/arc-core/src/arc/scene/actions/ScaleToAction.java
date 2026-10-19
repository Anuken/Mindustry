package arc.scene.actions;

/**
 * Sets the actor's scale from its current value to a specific value.
 * @author Nathan Sweet
 */
public class ScaleToAction extends TemporalAction{
    private float startX, startY;
    private float endX, endY;

    @Override
    protected void begin(){
        startX = target.scaleX;
        startY = target.scaleY;
    }

    @Override
    protected void update(float percent){
        target.setScale(startX + (endX - startX) * percent, startY + (endY - startY) * percent);
    }

    public void setScale(float x, float y){
        endX = x;
        endY = y;
    }

    public void setScale(float scale){
        endX = scale;
        endY = scale;
    }

    public float getX(){
        return endX;
    }

    public void setX(float x){
        this.endX = x;
    }

    public float getY(){
        return endY;
    }

    public void setY(float y){
        this.endY = y;
    }
}
