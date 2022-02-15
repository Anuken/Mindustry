package mindustry.world.blocks.power;

import mindustry.type.*;
import mindustry.world.draw.*;

//TODO deprecate this whole class?
public class BurnerGenerator extends ItemLiquidGenerator{

    public BurnerGenerator(String name){
        super(true, false, name);

        drawer = new DrawMulti(new DrawBlock(), new DrawWarmupRegion(), new DrawTurbines());
    }

    @Override
    protected float getLiquidEfficiency(Liquid liquid){
        return liquid.flammability;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.flammability;
    }

    public class BurnerGeneratorBuild extends ItemLiquidGeneratorBuild{

    }
}
