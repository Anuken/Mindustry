package mindustry.world.blocks.power;

import mindustry.type.*;

public class SingleTypeGenerator extends ItemLiquidGenerator{
    public boolean useItems = true;

    public SingleTypeGenerator(String name){
        super(name);
        defaults = true;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return useItems ? 1f : 0f;
    }

    @Override
    protected float getLiquidEfficiency(Liquid liquid){
        return useItems ? 0f : 1f;
    }
}
