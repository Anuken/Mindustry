package arc.scene.actions;

/**
 * Base class for actions that transition over time using the percent complete since the last frame.
 * @author Nathan Sweet
 */
abstract public class RelativeTemporalAction extends TemporalAction{
    private float lastPercent;

    @Override
    protected void begin(){
        lastPercent = 0;
    }

    @Override
    protected void update(float percent){
        updateRelative(percent - lastPercent);
        lastPercent = percent;
    }

    abstract protected void updateRelative(float percentDelta);
}
