package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.world.Tile;

public interface SpawnerTrait extends TargetTrait{
    Tile getTile();

    void updateSpawning(Player unit);

    @Override
    default boolean isValid(){
        return getTile().entity instanceof SpawnerTrait;
    }
}
