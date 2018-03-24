package io.anuke.mindustry.world.blocks.types.generation;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class PowerGenerator extends PowerBlock {
    protected float powerSpeed = 1f;

    public PowerGenerator(String name) {
        super(name);
    }

    protected void distributePower(Tile tile){
        //TODO!
    }

    @Override
    public void onDestroyed(Tile tile){
        float x = tile.worldx(), y = tile.worldy();

        Effects.effect(Fx.shellsmoke, x, y);
        Effects.effect(Fx.blastsmoke, x, y);

        Timers.run(Mathf.random(8f + Mathf.random(6f)), () -> {
            Effects.shake(6f, 8f, x, y);
            Effects.effect(Fx.generatorexplosion, x, y);
            Effects.effect(Fx.shockwave, x, y);

            //TODO better explosion effect!

            Effects.sound(explosionSound, x, y);
        });
    }

    @Override
    public TileEntity getEntity() {
        return new GeneratorEntity();
    }

    public static class GeneratorEntity extends TileEntity{
        public float generateTime;
    }
}
