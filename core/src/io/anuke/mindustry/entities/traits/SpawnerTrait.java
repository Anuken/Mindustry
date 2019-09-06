package io.anuke.mindustry.entities.traits;

import io.anuke.arc.math.geom.Position;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.world.Tile;

public interface SpawnerTrait extends TargetTrait, Position{
    Tile getTile();

    void updateSpawning(Player unit);

    boolean hasUnit(Unit unit);

    @Override
    default boolean isValid(){
        return getTile().entity instanceof SpawnerTrait;
    }
}
