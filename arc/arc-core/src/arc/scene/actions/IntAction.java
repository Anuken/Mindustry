package arc.scene.actions;

/**
 * An action that has an int, whose value is transitioned over time.
 * @author Nathan Sweet
 */
public class IntAction extends TemporalAction{
    private int start, end;
    private int value;

    /** Creates an IntAction that transitions from 0 to 1. */
    public IntAction(){
        start = 0;
        end = 1;
    }

    /** Creates an IntAction that transitions from start to end. */
    public IntAction(int start, int end){
        this.start = start;
        this.end = end;
    }

    @Override
    protected void begin(){
        value = start;
    }

    @Override
    protected void update(float percent){
        value = (int)(start + (end - start) * percent);
    }

    /** Gets the current int value. */
    public int getValue(){
        return value;
    }

    /** Sets the current int value. */
    public void setValue(int value){
        this.value = value;
    }

    public int getStart(){
        return start;
    }

    /** Sets the value to transition from. */
    public void setStart(int start){
        this.start = start;
    }

    public int getEnd(){
        return end;
    }

    /** Sets the value to transition to. */
    public void setEnd(int end){
        this.end = end;
    }
}
