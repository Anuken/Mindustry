package io.anuke.mindustry.entities.units;

public interface UnitState {
    default void entered(BaseUnit unit){}
    default void exited(BaseUnit unit){}
    default void update(BaseUnit unit){}
}
