package mindustry.world.blocks;

import mindustry.gen.*;

/** Any block that has a proxy unit that can be controlled by a player. */
public interface ControlBlock{
    Unit unit();

    /** @return whether this block is being controlled by a player. */
    default boolean isControlled(){
        return unit().isPlayer();
    }
}
