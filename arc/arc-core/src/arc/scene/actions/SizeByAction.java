package arc.scene.actions;

/**
 * Moves an actor from its current size to a relative size.
 * @author Nathan Sweet
 */
public class SizeByAction extends RelativeTemporalAction{
    private float amountWidth, amountHeight;

    @Override
    protected void updateRelative(float percentDelta){
        target.sizeBy(amountWidth * percentDelta, amountHeight * percentDelta);
    }

    public void setAmount(float width, float height){
        amountWidth = width;
        amountHeight = height;
    }

    public float getAmountWidth(){
        return amountWidth;
    }

    public void setAmountWidth(float width){
        amountWidth = width;
    }

    public float getAmountHeight(){
        return amountHeight;
    }

    public void setAmountHeight(float height){
        amountHeight = height;
    }
}
