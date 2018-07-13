package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.layout.Table;

public class ConsumePower extends Consume{
    private final float use;

    public ConsumePower(float use){
        this.use = use;
    }

    @Override
    public void buildTooltip(Table table){

    }

    @Override
    public String getIcon(){
        return "icon-power";
    }

    @Override
    public void update(Block block, TileEntity entity){
        entity.power.amount -= Math.min(use(block), entity.power.amount);
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        return entity.power.amount >= use(block);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(BlockStat.powerUse, use * 60f, StatUnit.powerSecond);
    }

    float use(Block block){
        return Math.min(use * Timers.delta(), block.powerCapacity);
    }
}
