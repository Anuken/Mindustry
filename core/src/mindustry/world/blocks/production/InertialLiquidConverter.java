package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.ui.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

public class InertialLiquidConverter extends LiquidConverter{

    public float warmupSpeed = 0.001f;

    public @Load("@-rotator") TextureRegion rotatorRegion;
    public @Load("@-bottom") TextureRegion bottomRegion;
    public @Load("@-liquid") TextureRegion liquidRegion;
    public @Load("@-top") TextureRegion topRegion;
    public InertialLiquidConverter(String name){
        super(name);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("liquidoutput", (InertialLiquidConverterBuild entity) -> new Bar(() ->
        Core.bundle.format("bar.liquidoutput",
        Strings.fixed(entity.use * entity.warmup * 60 * entity.timeScale() / Time.delta, 1)),
        () -> outputLiquid.liquid.barColor(),
        () -> entity.productionEfficiency));
    }

    public class InertialLiquidConverterBuild extends LiquidConverterBuild{
        public float spinRotation = 0.0f;
        public float warmup = 0.0f;
        public float use = 0.0f;
        /** The efficiency of the producer. An efficiency of 1.0 means 100% */
        public float productionEfficiency = 0.0f;

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(productionEfficiency);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            productionEfficiency = read.f();
        }

        @Override
        public void updateTile(){
            ConsumeLiquidBase cl = consumes.get(ConsumeType.liquid);

            if(cons.valid()){
                use = Math.min(cl.amount * edelta(), liquidCapacity - liquids.get(outputLiquid.liquid));

                warmup = Mathf.lerpDelta(warmup, 1f, warmupSpeed);
                if(Mathf.equal(warmup, 1f, 0.001f)){
                    warmup = 1f;
                }

                progress += use / cl.amount * warmup;
                liquids.add(outputLiquid.liquid, use * warmup);
                if(progress >= craftTime){
                    consume();
                    progress %= craftTime;
                }

            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.01f);
            }

            productionEfficiency = warmup;
            dumpLiquid(outputLiquid.liquid);
        }

        @Override
        public void draw() {
            Draw.rect(bottomRegion, x, y);

            Draw.color(outputLiquid.liquid.color);
            Draw.alpha(liquids.get(outputLiquid.liquid) / liquidCapacity);
            Draw.rect(liquidRegion, x, y);
            Draw.color();

            Draw.rect(region, x, y);

            spinRotation += Time.delta * warmup * warmup * 8;
            Draw.rect(rotatorRegion, x, y, spinRotation);
            Draw.rect(topRegion, x, y);
        }
    }
}
