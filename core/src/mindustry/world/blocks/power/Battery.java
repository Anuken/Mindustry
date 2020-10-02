package mindustry.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.meta.*;
import mindustry.world.Block;

import static mindustry.Vars.tilesize;

public class Battery extends PowerDistributor{
    public @Load("@-top") TextureRegion topRegion;

    public Color emptyLightColor = Color.valueOf("f8c266");
    public Color fullLightColor = Color.valueOf("fb9567");

    public Battery(String name){
        super(name);
        outputsPower = true;
        consumesPower = true;
        flags = EnumSet.of(BlockFlag.powerResupply);
    }

    @Override
    public boolean canReplace(Block other){
        if(other.alwaysReplace) return true;
        return (other != this || rotate) && this.group != BlockGroup.none && other.group == this.group && other != this && size >= other.size;
    }

    public class BatteryBuild extends Building{
        @Override
        public void draw(){
            Draw.color(emptyLightColor, fullLightColor, power.status);
            Fill.square(x, y, tilesize * size / 2f - 1);
            Draw.color();

            Draw.rect(topRegion, x, y);
        }
    }
}
