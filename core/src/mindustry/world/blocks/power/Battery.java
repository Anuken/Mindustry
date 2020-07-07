package mindustry.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

import static mindustry.Vars.tilesize;

public class Battery extends PowerDistributor{
    public @Load("@-top") TextureRegion topRegion;

    public Color emptyLightColor = Color.valueOf("f8c266");
    public Color fullLightColor = Color.valueOf("fb9567");

    public Battery(String name){
        super(name);
        outputsPower = true;
        consumesPower = true;
    }

    public class BatteryEntity extends Building{
        @Override
        public void draw(){
            Draw.color(emptyLightColor, fullLightColor, power.status);
            Fill.square(x, y, tilesize * size / 2f - 1);
            Draw.color();

            Draw.rect(topRegion, x, y);
        }
    }
}
