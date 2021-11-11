package mindustry.world.blocks.heat;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.NuclearReactor.*;
import mindustry.world.meta.*;

public class HeatProducer extends Block{
    public float heatOutput = 10f;
    public float warmupRate = 0.25f;
    public float consumeTime = 100;

    public @Load("@-heat") TextureRegion heatRegion;
    public @Load("@-glow") TextureRegion glowRegion;
    public @Load("@-top1") TextureRegion topRegion1;
    public @Load("@-top2") TextureRegion topRegion2;
    public Color heatColor = new Color(1f, 0.22f, 0.22f, 0.8f);
    public float heatPulse = 0.3f, heatPulseScl = 10f, glowMult = 1.2f;

    public HeatProducer(String name){
        super(name);

        update = solid = rotate = true;
        canOverdrive = false;
    }

    @Override
    public void setStats(){
        stats.timePeriod = consumeTime;
        super.setStats();
        stats.add(Stat.productionTime, consumeTime / 60f, StatUnit.seconds);
        //TODO heat prod stats
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("heat", (NuclearReactorBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> entity.heat));
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(req.rotation > 1 ? topRegion2 : topRegion1, req.drawx(), req.drawy(), req.rotation * 90);
    }


    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion1};
    }

    public class HeatProducerBuild extends Building implements HeatBlock{
        public float heat;
        public float progress;

        @Override
        public void updateTile(){
            if(consValid()){
                progress += getProgressIncrease(consumeTime);

                if(progress >= 1f){
                    consume();
                    progress -= 1f;
                }
            }

            //heat approaches target at the same speed regardless of efficiency
            heat = Mathf.approachDelta(heat, heatOutput * efficiency() * Mathf.num(consValid()), warmupRate * delta());
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            Draw.rect(rotation > 1 ? topRegion2 : topRegion1, x, y, rotdeg());

            if(heat > 0){
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                Draw.color(heatColor, heat / heatOutput * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse))));
                Draw.rect(heatRegion, x, y, rotdeg());
                Draw.color(Draw.getColor().mul(glowMult));
                Draw.rect(glowRegion, x, y);
                Draw.blend();
                Draw.color();
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return Mathf.clamp(progress);
            return super.sense(sensor);
        }

        @Override
        public boolean shouldAmbientSound(){
            return cons.valid();
        }

        @Override
        public float heat(){
            return heat;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.f(heat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            heat = read.f();
        }
    }
}
