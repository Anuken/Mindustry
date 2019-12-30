package mindustry.world.blocks.power;

import mindustry.type.Item;
import mindustry.type.Liquid;

public class BurnerGenerator extends ItemLiquidGenerator{

    public BurnerGenerator(String name){
        super(true, false, name);
    }

    @Override
    protected float getLiquidEfficiency(Liquid liquid){
        return liquid.flammability;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.flammability;
    }
}
