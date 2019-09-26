package io.anuke.mindustry.world.blocks.production;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeLiquidBase;
import io.anuke.mindustry.world.consumers.ConsumeType;
import io.anuke.mindustry.world.meta.BlockStat;

public class LiquidConverter extends GenericCrafter{

    public LiquidConverter(String name){
        super(name);
        hasLiquids = true;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void init(){
        ConsumeLiquidBase cl = consumes.get(ConsumeType.liquid);
        cl.update(true);
        outputLiquid.amount = cl.amount;
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(BlockStat.output);
        stats.add(BlockStat.output, outputLiquid.liquid, outputLiquid.amount * craftTime, false);
    }

    @Override
    public void update(Tile tile){
        GenericCrafterEntity entity = tile.entity();
        ConsumeLiquidBase cl = consumes.get(ConsumeType.liquid);

        if(tile.entity.cons.valid()){
            float use = Math.min(cl.amount * entity.delta(), liquidCapacity - entity.liquids.get(outputLiquid.liquid));
            if(hasPower){
                use *= entity.power.satisfaction; // Produce less liquid if power is not maxed
            }
            useContent(tile, outputLiquid.liquid);
            entity.progress += use / cl.amount / craftTime;
            entity.liquids.add(outputLiquid.liquid, use);
            if(entity.progress >= 1f){
                entity.cons.trigger();
                entity.progress = 0f;
            }
        }

        tryDumpLiquid(tile, outputLiquid.liquid);
    }
}
