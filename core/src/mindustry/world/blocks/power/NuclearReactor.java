package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class NuclearReactor extends PowerGenerator{
    public final int timerFuel = timers++;

    public final Vec2 tr = new Vec2();

    public Color lightColor = Color.valueOf("7f19ea");
    public Color coolColor = new Color(1, 1, 1, 0f);
    public Color hotColor = Color.valueOf("ff9575a3");
    public Effect explodeEffect = Fx.reactorExplosion;
    /** ticks to consume 1 fuel */
    public float itemDuration = 120;
    /** heating per frame * fullness */
    public float heating = 0.01f;
    /** threshold at which block starts smoking */
    public float smokeThreshold = 0.3f;
    /** heat threshold at which lights start flashing */
    public float flashThreshold = 0.46f;
    public int explosionRadius = 19;
    public int explosionDamage = 1250;
    /** heat removed per unit of coolant */
    public float coolantPower = 0.5f;

    public @Load("@-top") TextureRegion topRegion;
    public @Load("@-lights") TextureRegion lightsRegion;

    public NuclearReactor(String name){
        super(name);
        itemCapacity = 30;
        liquidCapacity = 30;
        hasItems = true;
        hasLiquids = true;
        rebuildable = false;
        flags = EnumSet.of(BlockFlag.reactor, BlockFlag.generator);
        schematicPriority = -5;
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(Stat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("heat", (NuclearReactorBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> entity.heat));
    }

    public class NuclearReactorBuild extends GeneratorBuild{
        public float heat;

        @Override
        public void updateTile(){
            ConsumeLiquid cliquid = consumes.get(ConsumeType.liquid);
            Item item = consumes.getItem().items[0].item;

            int fuel = items.get(item);
            float fullness = (float)fuel / itemCapacity;
            productionEfficiency = fullness;

            if(fuel > 0 && enabled){
                heat += fullness * heating * Math.min(delta(), 4f);

                if(timer(timerFuel, itemDuration / timeScale)){
                    consume();
                }
            }else{
                productionEfficiency = 0f;
            }

            Liquid liquid = cliquid.liquid;

            if(heat > 0){
                float maxUsed = Math.min(liquids.get(liquid), heat / coolantPower);
                heat -= maxUsed * coolantPower;
                liquids.remove(liquid, maxUsed);
            }

            if(heat > smokeThreshold){
                float smoke = 1.0f + (heat - smokeThreshold) / (1f - smokeThreshold); //ranges from 1.0 to 2.0
                if(Mathf.chance(smoke / 20.0 * delta())){
                    Fx.reactorsmoke.at(x + Mathf.range(size * tilesize / 2f),
                    y + Mathf.range(size * tilesize / 2f));
                }
            }

            heat = Mathf.clamp(heat);

            if(heat >= 0.999f){
                Events.fire(Trigger.thoriumReactorOverheat);
                kill();
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.heat) return heat;
            return super.sense(sensor);
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();

            Sounds.explosionbig.at(tile);

            int fuel = items.get(consumes.<ConsumeItems>get(ConsumeType.item).items[0].item);

            if((fuel < 5 && heat < 0.5f) || !state.rules.reactorExplosions) return;

            Effect.shake(6f, 16f, x, y);
            Damage.damage(x, y, explosionRadius * tilesize, explosionDamage * 4);

            explodeEffect.at(x, y);
        }

        @Override
        public void drawLight(){
            float fract = productionEfficiency;
            Drawf.light(team, x, y, (90f + Mathf.absin(5, 5f)) * fract, Tmp.c1.set(lightColor).lerp(Color.scarlet, heat), 0.6f * fract);
        }

        @Override
        public void draw(){
            super.draw();

            Draw.color(coolColor, hotColor, heat);
            Fill.rect(x, y, size * tilesize, size * tilesize);

            Draw.color(liquids.current().color);
            Draw.alpha(liquids.currentAmount() / liquidCapacity);
            Draw.rect(topRegion, x, y);

            if(heat > flashThreshold){
                float flash = 1f + ((heat - flashThreshold) / (1f - flashThreshold)) * 5.4f;
                flash += flash * Time.delta;
                Draw.color(Color.red, Color.yellow, Mathf.absin(flash, 9f, 1f));
                Draw.alpha(0.6f);
                Draw.rect(lightsRegion, x, y);
            }

            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
        }
    }
}
