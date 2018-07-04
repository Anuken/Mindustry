package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.ucore.core.Timers;

public class PowerDistributor extends PowerBlock {

    public PowerDistributor(String name) {
        super(name);
    }

    protected void distributePower(Tile tile){
        TileEntity entity = tile.entity;
        int sources = 0;

        if(entity == null) return;

        if(Float.isNaN(entity.power.amount)){
            entity.power.amount = 0f;
        }

        for(GridPoint2 point : Edges.getEdges(size)){
            Tile target = tile.getNearby(point);
            if(target != null && target.block().hasPower &&
                    shouldDistribute(tile, target)) sources ++;
        }

        if(sources == 0) return;

        float result = entity.power.amount / sources;

        for(GridPoint2 point : Edges.getEdges(size)){
            Tile target = tile.getNearby(point);
            if(target == null) continue;
            target = target.target();

            if(target.block().hasPower && shouldDistribute(tile, target)){
                float diff = (tile.entity.power.amount / powerCapacity - target.entity.power.amount / target.block().powerCapacity)/1.4f;

                float transmit = Math.min(result * Timers.delta(), diff * powerCapacity);
                if(target.block().acceptPower(target, tile, transmit)){
                    float transferred = target.block().addPower(target, transmit);
                    entity.power.amount -= transferred;
                }
            }
        }
    }

    protected boolean shouldDistribute(Tile tile, Tile other) {
        //only generators can distribute to other generators
        return (!(other.block() instanceof PowerGenerator) || tile.block() instanceof PowerGenerator)
                && other.entity.power.amount / other.block().powerCapacity < tile.entity.power.amount / powerCapacity;
    }

    @Override
    public void update(Tile tile) {
        distributePower(tile);
    }
}
