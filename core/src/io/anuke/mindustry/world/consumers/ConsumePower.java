package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.scene.ui.layout.Table;

public class ConsumePower extends Consume{
    protected final float use;

    public ConsumePower(float use){
        this.use = use;
    }

    @Override
    public void buildTooltip(Table table){

    }

    @Override
    public String getIcon(){
        return "icon-power-small";
    }

    @Override
    public void update(Block block, TileEntity entity){
        if(entity.power == null) return;
        entity.power.amount -= Math.min(use(block, entity), entity.power.amount);
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        return entity.power != null && entity.power.amount >= use(block, entity);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(BlockStat.powerUse, use * 60f, StatUnit.powerSecond);
    }

    protected float use(Block block, TileEntity entity){
        return Math.min(use * entity.delta(), block.powerCapacity);
    }
}
