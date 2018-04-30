package io.anuke.mindustry.entities.units;

public class StateMachine {
    private UnitState state;

    public void update(BaseUnit unit){
        if(state != null) state.update(unit);
    }

    public void set(BaseUnit unit, UnitState next){
        if(next == state) return;
        if(state != null) state.exited(unit);
        this.state = next;
        if(next != null) next.entered(unit);
    }
}
