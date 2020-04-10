package mindustry.world.blocks.power;

import mindustry.type.Item;
import mindustry.type.Liquid;

public class SingleTypeGenerator extends ItemLiquidGenerator{

    public SingleTypeGenerator(String name){
        super(name);
        defaults = true;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return 1f;
    }

    @Override
    protected float getLiquidEfficiency(Liquid liquid){
        return 0f;
    }
}
