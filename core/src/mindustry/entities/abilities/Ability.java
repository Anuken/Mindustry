package mindustry.entities.abilities;

import mindustry.gen.*;

public interface Ability{
    default void update(Unit unit){}
    default void draw(Unit unit){}
}
