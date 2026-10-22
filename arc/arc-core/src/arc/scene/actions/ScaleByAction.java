package arc.scene.actions;

/**
 * Scales an actor's scale to a relative size.
 * @author Nathan Sweet
 */
public class ScaleByAction extends RelativeTemporalAction{
    private float amountX, amountY;

    public ScaleByAction(){
    }

    public ScaleByAction(float amount){
        setAmount(amount);
    }

    @Override
    protected void updateRelative(float percentDelta){
        target.scaleBy(amountX * percentDelta, amountY * percentDelta);
    }

    public void setAmount(float x, float y){
        amountX = x;
        amountY = y;
    }

    public void setAmount(float scale){
        amountX = scale;
        amountY = scale;
    }

    public float getAmountX(){
        return amountX;
    }

    public void setAmountX(float x){
        this.amountX = x;
    }

    public float getAmountY(){
        return amountY;
    }

    public void setAmountY(float y){
        this.amountY = y;
    }

}
