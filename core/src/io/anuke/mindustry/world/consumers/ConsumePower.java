package io.anuke.mindustry.world.consumers;

import io.anuke.ucore.scene.ui.layout.Table;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatUnit;

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
        return "icon-power";
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        return entity.power.satisfaction >= 1;
    }

    @Override
    public void display(BlockStats stats){
        stats.add(BlockStat.powerUse, use * 60f, StatUnit.powerSecond);
    }

    public float getUse(Block block, TileEntity entity){
        return use * entity.delta();
    }

    public void addPower(float amount) {
        entity.power.satisfaction = amount / getUse();
    }
}
