package mindustry.world.blocks.power;

import mindustry.type.Item;
import mindustry.type.Liquid;

public class PowerCellConverter extends ItemLiquidGenerator{

    public PowerCellConverter(String name){
        super(name);
        defaults = true;
        this.hasLiquids = false;
        this.randomlyExplode = false;
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
