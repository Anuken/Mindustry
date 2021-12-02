package mindustry.world.blocks.power;

import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.type.*;
import mindustry.world.draw.*;

//TODO deprecate this whole class?
public class BurnerGenerator extends ItemLiquidGenerator{
    @Deprecated
    public @Load(value = "@-turbine#", length = 2) TextureRegion[] turbineRegions;
    @Deprecated
    public @Load("@-cap") TextureRegion capRegion;
    @Deprecated
    public float turbineSpeed = 2f;

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
