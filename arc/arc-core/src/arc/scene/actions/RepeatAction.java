package arc.scene.actions;

/**
 * Repeats an action a number of times or forever.
 * @author Nathan Sweet
 */
public class RepeatAction extends DelegateAction{
    public static final int FOREVER = -1;

    private int repeatCount, executedCount;
    private boolean finished;

    @Override
    protected boolean delegate(float delta){
        if(executedCount == repeatCount) return true;
        if(action.act(delta)){
            if(finished) return true;
            if(repeatCount > 0) executedCount++;
            if(executedCount == repeatCount) return true;
            if(action != null) action.restart();
        }
        return false;
    }

    /** Causes the action to not repeat again. */
    public void finish(){
        finished = true;
    }

    @Override
    public void restart(){
        super.restart();
        executedCount = 0;
        finished = false;
    }

    public int getCount(){
        return repeatCount;
    }

    /** Sets the number of times to repeat. Can be set to {@link #FOREVER}. */
    public void setCount(int count){
        this.repeatCount = count;
    }
}
