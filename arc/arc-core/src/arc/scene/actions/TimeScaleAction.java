package arc.scene.actions;

/**
 * Multiplies the delta of an action.
 * @author Nathan Sweet
 */
public class TimeScaleAction extends DelegateAction{
    private float scale;

    @Override
    protected boolean delegate(float delta){
        return action == null || action.act(delta * scale);
    }

    public float getScale(){
        return scale;
    }

    public void setScale(float scale){
        this.scale = scale;
    }
}
