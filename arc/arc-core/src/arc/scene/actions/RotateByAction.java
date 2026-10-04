package arc.scene.actions;

/**
 * Sets the actor's rotation from its current value to a relative value.
 * @author Nathan Sweet
 */
public class RotateByAction extends RelativeTemporalAction{
    private float amount;

    @Override
    protected void updateRelative(float percentDelta){
        target.rotateBy(amount * percentDelta);
    }

    public float getAmount(){
        return amount;
    }

    public void setAmount(float rotationAmount){
        amount = rotationAmount;
    }
}
