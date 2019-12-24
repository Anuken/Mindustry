package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.*;

public interface BlockState{
    default void entered(Tile tile){
    }

    default void exited(Tile tile){
    }

    default void update(Tile tile){
    }
}
