package mindustry.world.blocks.power;

import arc.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class PowerGenerator extends PowerDistributor{
    /** The amount of power produced per tick in case of an efficiency of 1.0, which represents 100%. */
    public float powerProduction;
    public Stat generationType = Stat.basePowerGeneration;
    public DrawBlock drawer = new DrawDefault();

    public int explosionRadius = 12;
    public int explosionDamage = 0;
    public Effect explodeEffect = Fx.none;
    public Sound explodeSound = Sounds.none;

    public int explosionPuddles = 10;
    public float explosionPuddleRange = tilesize * 2f;
    public float explosionPuddleAmount = 100f;
    public @Nullable Liquid explosionPuddleLiquid;
    public float explosionMinWarmup = 0f;

    public float explosionShake = 0f, explosionShakeDuration = 6f;

    public PowerGenerator(String name){
        super(name);
        sync = true;
        baseExplosiveness = 5f;
        flags = EnumSet.of(BlockFlag.generator);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    @Override
    public void load(){
        super.load();
        drawer.load(this);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(hasPower && outputsPower){
            addBar("power", (GeneratorBuild entity) -> new Bar(() ->
            Core.bundle.format("bar.poweroutput",
            Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale(), 1)),
            () -> Pal.powerBar,
            () -> entity.productionEfficiency));
        }
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    public class GeneratorBuild extends Building{
        public float generateTime;
        /** The efficiency of the producer. An efficiency of 1.0 means 100% */
        public float productionEfficiency = 0.0f;

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public float warmup(){
            return productionEfficiency;
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();

            if(state.rules.reactorExplosions){
                createExplosion();
            }
        }

        public boolean shouldExplode(){
            return warmup() >= explosionMinWarmup;
        }

        public void createExplosion(){
            if(shouldExplode()){
                if(explosionDamage > 0){
                    Damage.damage(x, y, explosionRadius * tilesize, explosionDamage);
                }

                explodeEffect.at(this);
                explodeSound.at(this);

                if(explosionPuddleLiquid != null){
                    for(int i = 0; i < explosionPuddles; i++){
                        Tmp.v1.trns(Mathf.random(360f), Mathf.random(explosionPuddleRange));
                        Tile tile = world.tileWorld(x + Tmp.v1.x, y + Tmp.v1.y);
                        Puddles.deposit(tile, explosionPuddleLiquid, explosionPuddleAmount);
                    }
                }

                if(explosionShake > 0){
                    Effect.shake(explosionShake, explosionShakeDuration, this);
                }
            }
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public float ambientVolume(){
            return Mathf.clamp(productionEfficiency);
        }

        @Override
        public float getPowerProduction(){
            return powerProduction * productionEfficiency;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(productionEfficiency);
            write.f(generateTime);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            productionEfficiency = read.f();
            if(revision >= 1){
                generateTime = read.f();
            }
        }
    }
}
