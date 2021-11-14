package mindustry.world.blocks.production;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/** @deprecated this class has no new functionality over GenericCrafter, use GenericCrafter with a DrawSmelter drawer instead. See vanilla smelter blocks. */
@Deprecated
public class GenericSmelter extends GenericCrafter{
    public Color flameColor = Color.valueOf("ffc999");
    public @Load("@-top") TextureRegion topRegion;
    public float flameRadius = 3f, flameRadiusIn = 1.9f, flameRadiusScl = 5f, flameRadiusMag = 2f, flameRadiusInMag = 1f;

    public GenericSmelter(String name){
        super(name);
        ambientSound = Sounds.smelter;
        ambientSoundVolume = 0.07f;
    }

    public class SmelterBuild extends GenericCrafterBuild{
        @Override
        public void draw(){
            super.draw();

            //draw glowing center
            if(warmup > 0f && flameColor.a > 0.001f){
                float g = 0.3f;
                float r = 0.06f;
                float cr = Mathf.random(0.1f);

                Draw.z(Layer.block + 0.01f);

                Draw.alpha(((1f - g) + Mathf.absin(Time.time, 8f, g) + Mathf.random(r) - r) * warmup);

                Draw.tint(flameColor);
                Fill.circle(x, y, flameRadius + Mathf.absin(Time.time, flameRadiusScl, flameRadiusMag) + cr);
                Draw.color(1f, 1f, 1f, warmup);
                Draw.rect(topRegion, x, y);
                Fill.circle(x, y, flameRadiusIn + Mathf.absin(Time.time, flameRadiusScl, flameRadiusInMag) + cr);

                Draw.color();
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, (60f + Mathf.absin(10f, 5f)) * warmup * size, flameColor, 0.65f);
        }
    }
}
