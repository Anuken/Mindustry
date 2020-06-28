package mindustry.world.blocks.production;

import mindustry.world.consumers.*;
import mindustry.world.meta.*;

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

    public class LiquidConverterEntity extends GenericCrafterEntity{
        @Override
        public void drawLight(){
            if(hasLiquids && drawLiquidLight && outputLiquid.liquid.lightColor.a > 0.001f){
                drawLiquidLight(outputLiquid.liquid, liquids.get(outputLiquid.liquid));
            }
        }

        @Override
        public void updateTile(){
            ConsumeLiquidBase cl = consumes.get(ConsumeType.liquid);

            if(cons.valid()){
                float use = Math.min(cl.amount * edelta(), liquidCapacity - liquids.get(outputLiquid.liquid));

                progress += use / cl.amount;
                liquids.add(outputLiquid.liquid, use);
                if(progress >= craftTime){
                    consume();
                    progress %= craftTime;
                }
            }

            dumpLiquid(outputLiquid.liquid);
        }
    }
}
