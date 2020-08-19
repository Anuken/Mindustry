package mindustry.world.blocks.power;

import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.type.*;

public class BurnerGenerator extends ItemLiquidGenerator{
    public @Load(value = "@-turbine#", length = 2) TextureRegion[] turbineRegions;
    public @Load("@-cap") TextureRegion capRegion;
    public float turbineSpeed = 2f;

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

    @Override
    public TextureRegion[] icons(){
        return turbineRegions[0].found() ? new TextureRegion[]{region, turbineRegions[0], turbineRegions[1], capRegion} : super.icons();
    }

    public class BurnerGeneratorBuild extends ItemLiquidGeneratorBuild{

        @Override
        public void draw(){
            super.draw();

            if(turbineRegions[0].found()){
                Draw.rect(turbineRegions[0], x, y, totalTime * turbineSpeed);
                Draw.rect(turbineRegions[1], x, y, -totalTime * turbineSpeed);

                Draw.rect(capRegion, x, y);
            }
        }
    }
}
