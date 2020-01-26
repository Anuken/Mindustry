package mindustry.entities.traits;

import arc.math.geom.Position;
import mindustry.entities.type.*;
import mindustry.world.Tile;

public interface SpawnerTrait extends TargetTrait, Position{
    Tile getTile();

    void updateSpawning(Player unit);

    boolean hasUnit(Unit unit);

    @Override
    default boolean isValid(){
        return getTile().entity instanceof SpawnerTrait;
    }
}
