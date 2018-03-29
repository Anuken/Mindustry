package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class Fracker extends SolidPump {
    protected Liquid inputLiquid;
    protected float inputLiquidUse;

    public Fracker(String name) {
        super(name);
    }

    @Override
    public void update(Tile tile) {
        FrackerEntity entity = tile.entity();

        if(entity.input >= inputLiquidUse * Timers.delta()){
            super.update(tile);
            entity.input -= inputLiquidUse * Timers.delta();
        }
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        if(liquid != inputLiquid){
            super.handleLiquid(tile, source, liquid, amount);
        }else{
            FrackerEntity entity = tile.entity();
            entity.input += amount;
        }
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        FrackerEntity entity = tile.entity();

        return (liquid == inputLiquid && entity.input < inputLiquidUse) || super.acceptLiquid(tile, source, liquid, amount);
    }

    @Override
    public TileEntity getEntity() {
        return new FrackerEntity();
    }

    public static class FrackerEntity extends TileEntity{
        public float input;
    }
}
