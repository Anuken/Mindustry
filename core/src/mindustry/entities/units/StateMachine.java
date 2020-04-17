package mindustry.entities.units;

public class StateMachine{
    private UnitState state;

    public void update(){
        if(state != null) state.update();
    }

    public void set(UnitState next){
        if(next == state) return;
        if(state != null) state.exited();
        this.state = next;
        if(next != null) next.entered();
    }

    public UnitState current(){
        return state;
    }

    public boolean is(UnitState state){
        return this.state == state;
    }

    public interface UnitState{
        default void entered(){
        }

        default void exited(){
        }

        default void update(){
        }
    }
}
