package io.anuke.mindustry.world.blocks.defense;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Wall;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class PhaseWall extends Wall {
    protected float regenSpeed = 0.25f;

    public PhaseWall(String name) {
        super(name);
        update = true;
    }

    @Override
    public void update(Tile tile) {
        tile.entity.health = Mathf.clamp(tile.entity.health + regenSpeed * Timers.delta(), 0f, health);
    }
}
