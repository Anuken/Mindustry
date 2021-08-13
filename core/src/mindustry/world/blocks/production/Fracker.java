package mindustry.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;

public class Fracker extends SolidPump{
    public float itemUseTime = 100f;

    public @Load("@-liquid") TextureRegion liquidRegion;
    public @Load("@-rotator") TextureRegion rotatorRegion;
    public @Load("@-top") TextureRegion topRegion;

    public Fracker(String name){
        super(name);
        hasItems = true;
        ambientSound = Sounds.drill;
        ambientSoundVolume = 0.03f;
        envRequired |= Env.groundOil;
    }

    @Override
    public void setStats(){
        stats.timePeriod = itemUseTime;
        super.setStats();

        stats.add(Stat.productionTime, itemUseTime / 60f, StatUnit.seconds);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, rotatorRegion, topRegion};
    }

    public class FrackerBuild extends SolidPumpBuild{
        public float accumulator;

        @Override
        public void drawCracks(){}

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            super.drawCracks();

            Drawf.liquid(liquidRegion, x, y, liquids.get(result) / liquidCapacity, result.color);

            Drawf.spinSprite(rotatorRegion, x, y, pumpTime);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void updateTile(){
            if(consValid()){
                if(accumulator >= itemUseTime){
                    consume();
                    accumulator -= itemUseTime;
                }

                super.updateTile();
                accumulator += delta() * efficiency();
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.02f);
                lastPump = 0f;
                dumpLiquid(result);
            }
        }

        @Override
        public float typeLiquid(){
            return liquids.get(result);
        }
    }
}
