package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.graphics.*;

public class LongPowerNode extends PowerNode{
    public @Load("@-glow") TextureRegion glow;
    public Color glowColor = Color.valueOf("cbfd81").a(0.45f);
    public float glowScl = 16f, glowMag = 0.6f;

    public LongPowerNode(String name){
        super(name);
        drawRange = false;
    }

    @Override
    public void load(){
        super.load();

        laser = Core.atlas.find("power-beam");
        laserEnd = Core.atlas.find("power-beam-end");
    }

    public class LongPowerNodeBuild extends PowerNodeBuild{
        public float warmup = 0f;

        @Override
        public void updateTile(){
            super.updateTile();

            warmup = Mathf.lerpDelta(warmup, power.links.size > 0 ? 1f : 0f, 0.05f);
        }

        @Override
        public void draw(){
            super.draw();

            if(warmup > 0.001f){
                Drawf.additive(glow, Tmp.c1.set(glowColor).mula(warmup).mula(1f - glowMag + Mathf.absin(glowScl, glowMag)), x, y);
            }
        }
    }
}
