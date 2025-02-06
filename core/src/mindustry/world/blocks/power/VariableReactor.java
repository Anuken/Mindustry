package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class VariableReactor extends PowerGenerator{
    public float maxHeat = 100f;
    /** How quickly instability moves towards 1, per frame. */
    public float unstableSpeed = 1f / 60f / 3f;
    public float warmupSpeed = 0.1f;
    public Effect effect = Fx.fluxVapor;
    public float effectChance = 0.05f;
    public Color effectColor = Color.valueOf("ffdf9d");

    public float flashThreshold = 0.01f, flashAlpha = 0.4f, flashSpeed = 7f;
    public Color flashColor1 = Color.red, flashColor2 = Color.valueOf("89e8b6");

    public @Load("@-lights") TextureRegion lightsRegion;

    public VariableReactor(String name){
        super(name);
        powerProduction = 20f;
        rebuildable = false;


        explosionRadius = 16;
        explosionDamage = 1500;
        explodeEffect = new MultiEffect(Fx.bigShockwave, new WrapEffect(Fx.titanSmoke, Color.valueOf("e3ae6f")));
        explodeSound = Sounds.explosionbig;

        explosionPuddles = 70;
        explosionPuddleRange = tilesize * 6f;
        explosionPuddleLiquid = Liquids.slag;
        explosionPuddleAmount = 100f;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("instability", (VariableReactorBuild entity) -> new Bar("bar.instability", Pal.sap, () -> entity.instability));

        addBar("heat", (VariableReactorBuild entity) ->
            new Bar(() ->
            Core.bundle.format("bar.heatpercent", (int)entity.heat, (int)(Mathf.clamp(entity.heat / maxHeat) * 100)),
            () -> Pal.lightOrange,
            () -> entity.heat / maxHeat));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.input, maxHeat, StatUnit.heatUnits);
    }

    //TODO: draw warmup fraction on block?
    public class VariableReactorBuild extends GeneratorBuild implements HeatConsumer{
        public float[] sideHeat = new float[4];
        public float heat = 0f, instability, totalProgress, warmup, flash;

        @Override
        public void updateTile(){
            heat = calculateHeat(sideHeat);

            productionEfficiency = efficiency;
            warmup = Mathf.lerpDelta(warmup, productionEfficiency > 0 ? 1f : 0f, warmupSpeed);

            if(instability >= 1f){
                kill();
            }

            totalProgress += productionEfficiency * Time.delta;

            if(Mathf.chanceDelta(effectChance * warmup)){
                effect.at(x, y, effectColor);
            }
        }

        @Override
        public boolean shouldExplode(){
            return heat > 0f;
        }

        @Override
        public void draw(){
            super.draw();

            if(instability > flashThreshold){
                if(!state.isPaused()) flash += (1f + ((instability - flashThreshold) / (1f - flashThreshold)) * flashSpeed) * Time.delta;
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                Draw.color(flashColor1, flashColor2, Mathf.absin(flash, 8f, 1f));
                Draw.alpha(flashAlpha * Mathf.clamp((instability - flashThreshold) / (1f - flashThreshold) * 4f));
                Draw.rect(lightsRegion, x, y);
                Draw.blend();
            }
        }

        @Override
        public float totalProgress(){
            return totalProgress;
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public void updateEfficiencyMultiplier(){
            //at this stage efficiency = how much coolant is provided

            //target efficiency value
            float target = Mathf.clamp(heat / maxHeat);

            //fraction of coolant provided (from what is needed)
            float efficiencyMet = Mathf.clamp(Mathf.zero(target) ? 1f : efficiency / target);
            boolean met = efficiencyMet >= 0.99999f;

            //if all requirements are met, instability moves toward 0 at 50% of speed
            //if requirements are not meant, instability approaches 1 at a speed scaled by how much efficiency is *not* met
            instability = Mathf.approachDelta(instability, met ? 0f : 1f, met ? 0.5f : unstableSpeed * (1f - efficiencyMet));

            //now scale efficiency by target, so it consumes less depending on heat
            efficiency *= target;
        }

        @Override
        public float[] sideHeat(){
            return sideHeat;
        }

        @Override
        public float heatRequirement(){
            return maxHeat;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(heat);
            write.f(instability);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            heat = read.f();
            instability = read.f();
            warmup = read.f();
        }
    }
}
