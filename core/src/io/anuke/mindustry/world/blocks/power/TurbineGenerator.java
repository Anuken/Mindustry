package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Timers;

public class TurbineGenerator extends BurnerGenerator {
    protected float auxLiquidUse = 0.1f;
    protected Liquid auxLiquid = Liquids.water;
    protected float auxLiquidCapacity = 10;

    public TurbineGenerator(String name) {
        super(name);
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.add(BlockStat.inputLiquidAux, auxLiquid);
    }

    @Override
    public void update(Tile tile) {
        TurbineEntity entity = tile.entity();
        float used = Math.min(auxLiquidUse * Timers.delta(), auxLiquidCapacity);

        if(entity.aux >= used){
            super.update(tile);
            entity.aux -= used;
        }
    }

    @Override
    public float handleAuxLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        TurbineEntity entity = tile.entity();
        if(liquid == auxLiquid){
            float accepted = Math.min(auxLiquidCapacity - entity.aux, amount);
            entity.aux += accepted;
            return accepted;
        }else {
            return 0;
        }
    }

    @Override
    public TileEntity getEntity() {
        return new TurbineEntity();
    }

    public class TurbineEntity extends ItemGeneratorEntity{
        public float aux;
    }
}
