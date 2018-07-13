package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.world.Tile;

public interface SpawnerTrait{
    Tile getTile();

    void updateSpawning(Unit unit);

    float getSpawnProgress();
}
