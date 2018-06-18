package io.anuke.mindustry.world.blocks.effect;

import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class MendingCore extends EffectCore {
    /**Mending speed as a percentage of block health, per frame.*/
    protected float mendSpeed = 0.8f;

    public MendingCore(String name) {
        super(name);
    }

    @Override
    public void update(Tile tile) {

        for (int dx = Math.max(-range + tile.x, 0); dx <= Math.min(range + tile.y, world.width() - 1); dx++) {
            for (int dy = Math.max(-range + tile.y, 0); dy <= Math.min(range + tile.y, world.height() - 1); dy++) {
                Tile other = world.tile(dx, dy);

                if(other.entity != null){
                    other.entity.health = Mathf.clamp(other.entity.health + 1f / other.block().health * mendSpeed * Timers.delta(), 0, other.block().health);
                }
            }
        }
    }
}
