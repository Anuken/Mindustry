package mindustry.world.blocks.power;

import mindustry.type.Item;

public class DecayGenerator extends ItemLiquidGenerator{

    public DecayGenerator(String name){
        super(true, false, name);
        hasItems = true;
        hasLiquids = false;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.radioactivity;
    }
}
