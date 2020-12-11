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
        if(!consumes.has(ConsumeType.liquid) || !(consumes.get(ConsumeType.liquid) instanceof ConsumeLiquid)){
            throw new RuntimeException("LiquidsConverters must have a ConsumeLiquid. Note that filters are not supported.");
        }

        ConsumeLiquid cl = consumes.get(ConsumeType.liquid);
        cl.update(false);
        outputLiquid.amount = cl.amount;
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.output);
        stats.add(Stat.output, outputLiquid.liquid, outputLiquid.amount * 60f, true);
    }

    public class LiquidConverterBuild extends GenericCrafterBuild{
        @Override
        public void drawLight(){
            if(hasLiquids && drawLiquidLight && outputLiquid.liquid.lightColor.a > 0.001f){
                drawLiquidLight(outputLiquid.liquid, liquids.get(outputLiquid.liquid));
            }
        }

        @Override
        public void updateTile(){
            ConsumeLiquid cl = consumes.get(ConsumeType.liquid);

            if(cons.valid()){
                float use = Math.min(cl.amount * edelta(), liquidCapacity - liquids.get(outputLiquid.liquid));

                liquids.remove(cl.liquid, Math.min(use, liquids.get(cl.liquid)));

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
