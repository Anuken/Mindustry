package mindustry.world.blocks.production;

import mindustry.world.Tile;
import mindustry.world.consumers.ConsumeLiquidBase;
import mindustry.world.consumers.ConsumeType;
import mindustry.world.meta.BlockStat;

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
    public void drawLight(Tile tile){
        if(hasLiquids && drawLiquidLight && outputLiquid.liquid.lightColor.a > 0.001f){
            drawLiquidLight(tile, outputLiquid.liquid, tile.entity.liquids.get(outputLiquid.liquid));
        }
    }

    @Override
    public void update(Tile tile){
        GenericCrafterEntity entity = tile.ent();
        ConsumeLiquidBase cl = consumes.get(ConsumeType.liquid);

        if(tile.entity.cons.valid()){
            float use = Math.min(cl.amount * entity.delta(), liquidCapacity - entity.liquids.get(outputLiquid.liquid)) * entity.efficiency();

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
