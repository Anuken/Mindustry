package mindustry.world.blocks;

import mindustry.gen.*;

/** Any block that has a proxy unit that can be controlled by a player. */
public interface ControlBlock{
    Unit unit();

    /** @return whether this block is being controlled by a player. */
    default boolean isControlled(){
        return unit().isPlayer();
    }

    /** @return whether this block can be controlled at all. */
    default boolean canControl(){
        return true;
    }

    /** @return whether targets should automatically be selected (on mobile) */
    default boolean shouldAutoTarget(){
        return true;
    }
}
