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
    public void drawLight(){
        if(hasLiquids && drawLiquidLight && outputLiquid.liquid.lightColor.a > 0.001f){
            drawLiquidLight(tile, outputLiquid.liquid, tile.liquids.get(outputLiquid.liquid));
        }
    }

    @Override
    public void updateTile(){
        ConsumeLiquidBase cl = consumes.get(ConsumeType.liquid);

        if(tile.cons().valid()){
            float use = Math.min(cl.amount * delta(), liquidCapacity - liquids.get(outputLiquid.liquid)) * efficiency();

            useContent(tile, outputLiquid.liquid);
            progress += use / cl.amount / craftTime;
            liquids.add(outputLiquid.liquid, use);
            if(progress >= 1f){
                consume();
                progress = 0f;
            }
        }

        tryDumpLiquid(tile, outputLiquid.liquid);
    }
}
