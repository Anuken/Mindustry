package mindustry.world.blocks.production;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;

public class BurstDrill extends Drill{
    public float shake = 2f;
    public Interp speedCurve = Interp.pow2In;

    public @Load("@-top-invert") TextureRegion topInvertRegion;
    public @Load("@-arrow") TextureRegion arrowRegion;
    public @Load("@-arrow-blur") TextureRegion arrowBlurRegion;
    public float invertTime = 80f;
    public float arrowSpacing = 4f;
    public Color arrowColor = Color.valueOf("feb380");
    //public @Load(value = "impact-reactor-plasma-#", length = 4) TextureRegion[] plasmaRegions;
    //public Color plasma1 = Color.valueOf("ffd06b"), plasma2 = Color.valueOf("ff361b");

    public BurstDrill(String name){
        super(name);

        itemCapacity = 20;
        //does not drill in the traditional sense, so this is not even used
        hardnessDrillMultiplier = 0f;
        //generally at center
        drillEffectRnd = 0f;
        drillEffect = Fx.shockwave;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    public class BurstDrillBuild extends DrillBuild{
        //used so the lights don't fade out immediately
        public float smoothProgress = 0f;
        public float invertTime = 0f;

        @Override
        public void updateTile(){
            if(dominantItem == null){
                return;
            }

            if(invertTime > 0f) invertTime -= delta() / invertTime;

            if(timer(timerDump, dumpTime)){
                dump(items.has(dominantItem) ? dominantItem : null);
            }

            smoothProgress = Mathf.lerpDelta(smoothProgress, progress / (drillTime - 20f), 0.1f);

            if(items.total() <= itemCapacity - dominantItems && dominantItems > 0 && consValid()){
                warmup = Mathf.approachDelta(warmup, progress / drillTime, 0.01f);

                float speed = efficiency();

                timeDrilled += speedCurve.apply(progress / drillTime) * speed;

                lastDrillSpeed = 1f / drillTime * speed;
                progress += delta() * speed;
            }else{
                warmup = Mathf.approachDelta(warmup, 0f, 0.01f);
                lastDrillSpeed = 0f;
                return;
            }

            if(dominantItems > 0 && progress >= drillTime && items.total() < itemCapacity){
                for(int i = 0; i < dominantItems; i++){
                    offload(dominantItem);
                }

                invertTime = 1f;
                Effect.shake(shake, shake, this);
                progress %= drillTime;
                drillEffect.at(x + Mathf.range(drillEffectRnd), y + Mathf.range(drillEffectRnd), dominantItem.color);
            }
        }

        @Override
        public boolean shouldConsume(){
            return items.total() <= itemCapacity - dominantItems && enabled;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            drawDefaultCracks();

            //TODO charge

            if(dominantItem != null && drawMineItem){
                Draw.color(dominantItem.color);
                Draw.rect(itemRegion, x, y);
                Draw.color();
            }

            /*
            Draw.blend(Blending.additive);
            for(int i = 0; i < plasmaRegions.length; i++){
                float r = size * tilesize - 3f + Mathf.absin(timeDrilled, 2f + i * 1f, 5f - i * 0.5f);

                Draw.color(plasma1, plasma2, (float)i / plasmaRegions.length);
                Draw.alpha((0.3f + Mathf.absin(Time.time, 2f + i * 2f, 0.3f + i * 0.05f)) * Interp.pow4In.apply(warmup));
                Draw.rect(plasmaRegions[i], x, y, r, r, timeDrilled  * 3f * (12 + i * 2f));
            }
            Draw.blend();
            Draw.color();*/

            //Drawf.spinSprite(rotatorRegion, x, y, timeDrilled * rotateSpeed);
            Draw.rect(topRegion, x, y);
            if(invertTime > 0){
                Draw.alpha(Interp.pow3Out.apply(invertTime));
                Draw.rect(topInvertRegion, x, y);
                Draw.color();
            }

            float z = Draw.z();

            float fract = smoothProgress;
            int arrows = 3;
            Draw.color(arrowColor);
            for(int i = 0; i < 4; i++){
                for(int j = 0; j < arrows; j++){
                    float arrowFract = (arrows - 1 - j);
                    float a = Mathf.clamp(fract * arrows - arrowFract);
                    Tmp.v1.trns(i * 90 + 45, j * arrowSpacing);

                    Draw.z(z);
                    Draw.color(Color.valueOf("6e7080"), arrowColor, a);
                    Draw.rect(arrowRegion, x + Tmp.v1.x, y + Tmp.v1.y, i * 90);

                    Draw.color(arrowColor);

                    Draw.z(z + 0.001f);
                    Draw.blend(Blending.additive);
                    Draw.alpha(Mathf.pow(a, 10f));
                    Draw.rect(arrowBlurRegion, x + Tmp.v1.x, y + Tmp.v1.y, i * 90);
                    Draw.blend();
                }
            }
            Draw.color();
        }
    }
}
