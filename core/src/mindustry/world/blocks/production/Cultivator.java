package mindustry.world.blocks.production;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.world.meta.*;

/**
 * @deprecated use GenericCrafter or AttributeCrafter with a DrawCultivator instead.
 * WARNING: If you switch from a class that used Cultivator to a GenericCrafter, make sure you set legacyReadWarmup to true! Failing to do so will break saves.
 * This class has been gutted of its behavior.
 * */
@Deprecated
public class Cultivator extends GenericCrafter{
    //fields are kept for compatibility
    public Color plantColor = Color.valueOf("5541b1");
    public Color plantColorLight = Color.valueOf("7457ce");
    public Color bottomColor = Color.valueOf("474747");

    public @Load("@-middle") TextureRegion middleRegion;
    public @Load("@-top") TextureRegion topRegion;
    public Rand random = new Rand(0);
    public float recurrence = 6f;
    public Attribute attribute = Attribute.spores;

    public Cultivator(String name){
        super(name);
    }

    public class CultivatorBuild extends GenericCrafterBuild{
        //compat
        public float warmup, boost;

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            warmup = read.f();
        }
    }
}
