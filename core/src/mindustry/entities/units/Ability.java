package mindustry.entities.units;

import mindustry.gen.*;

public interface Ability{
    default void update(Unit unit){}
    default void draw(Unit unit){}
}
