package io.anuke.mindustry.world.blocks.types.power;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.EnumSet;

public class PowerGenerator extends PowerBlock {

    public PowerGenerator(String name) {
        super(name);
        baseExplosiveness = 5f;
        flags = EnumSet.of(BlockFlag.producer);
    }

    protected void distributePower(Tile tile){
        TileEntity entity = tile.entity;
        int sources = 0;

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
                float transmit = Math.min(result * Timers.delta(), entity.power.amount);
                if(target.block().acceptPower(target, tile, transmit)){
                    entity.power.amount -= target.block().addPower(target, transmit);
                }
            }
        }
    }

    protected boolean shouldDistribute(Tile tile, Tile other){
        if(other.block() instanceof PowerGenerator){
            return other.entity.power.amount / other.block().powerCapacity <
                    tile.entity.power.amount / powerCapacity;
        }
        return true;
    }

    @Override
    public void update(Tile tile) {
        distributePower(tile);
    }

    @Override
    public TileEntity getEntity() {
        return new GeneratorEntity();
    }

    public static class GeneratorEntity extends TileEntity{
        public float generateTime;
        public float uptime;
    }
}
