package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class ImpactReactor extends PowerGenerator{
    public final int timerUse = timers++;

    public int plasmas = 4;
    public float warmupSpeed = 0.001f;
    public float itemDuration = 60f;
    public int explosionRadius = 50;
    public int explosionDamage = 2000;

    public Color plasma1 = Color.valueOf("ffd06b"), plasma2 = Color.valueOf("ff361b");
    public int bottomRegion;
    public int[] plasmaRegions;

    public ImpactReactor(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        liquidCapacity = 30f;
        hasItems = true;
        outputsPower = consumesPower = true;
        entityType = FusionReactorEntity::new;

        bottomRegion = reg("-bottom");
        plasmaRegions = new int[plasmas];
        for(int i = 0; i < plasmas; i++){
            plasmaRegions[i] = reg("-plasma-" + i);
        }
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("poweroutput", entity -> new Bar(() ->
        Core.bundle.format("bar.poweroutput",
        Strings.fixed(Math.max(entity.block.getPowerProduction(entity.tile) - consumes.getPower().usage, 0) * 60 * entity.timeScale, 1)),
        () -> Pal.powerBar,
        () -> ((GeneratorEntity)entity).productionEfficiency));
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(BlockStat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }
    }

    @Override
    public void update(Tile tile){
        FusionReactorEntity entity = tile.ent();

        if(entity.cons.valid() && entity.power.status >= 0.99f){
            boolean prevOut = getPowerProduction(tile) <= consumes.getPower().requestedPower(entity);

            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, warmupSpeed);
            if(Mathf.equal(entity.warmup, 1f, 0.001f)){
                entity.warmup = 1f;
            }

            if(!prevOut && (getPowerProduction(tile) > consumes.getPower().requestedPower(entity))){
                Events.fire(Trigger.impactPower);
            }

            if(entity.timer.get(timerUse, itemDuration / entity.timeScale)){
                entity.cons.trigger();
            }
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.01f);
        }

        entity.productionEfficiency = Mathf.pow(entity.warmup, 5f);
    }

    @Override
    public void draw(Tile tile){
        FusionReactorEntity entity = tile.ent();

        Draw.rect(reg(bottomRegion), tile.drawx(), tile.drawy());

        for(int i = 0; i < plasmas; i++){
            float r = 29f + Mathf.absin(Time.time(), 2f + i * 1f, 5f - i * 0.5f);

            Draw.color(plasma1, plasma2, (float)i / plasmas);
            Draw.alpha((0.3f + Mathf.absin(Time.time(), 2f + i * 2f, 0.3f + i * 0.05f)) * entity.warmup);
            Draw.blend(Blending.additive);
            Draw.rect(reg(plasmaRegions[i]), tile.drawx(), tile.drawy(), r, r, Time.time() * (12 + i * 6f) * entity.warmup);
            Draw.blend();
        }

        Draw.color();

        Draw.rect(region, tile.drawx(), tile.drawy());

        Draw.color();
    }

    @Override
    public void drawLight(Tile tile){
        float fract = tile.<FusionReactorEntity>ent().warmup;
        renderer.lights.add(tile.drawx(), tile.drawy(), (110f + Mathf.absin(5, 5f)) * fract, Tmp.c1.set(plasma2).lerp(plasma1, Mathf.absin(7f, 0.2f)), 0.8f * fract);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-bottom"), Core.atlas.find(name)};
    }

    @Override
    public void onDestroyed(Tile tile){
        super.onDestroyed(tile);

        FusionReactorEntity entity = tile.ent();

        if(entity.warmup < 0.4f || !state.rules.reactorExplosions) return;

        Sounds.explosionbig.at(tile);

        Effects.shake(6f, 16f, tile.worldx(), tile.worldy());
        Effects.effect(Fx.impactShockwave, tile.worldx(), tile.worldy());
        for(int i = 0; i < 6; i++){
            Time.run(Mathf.random(80), () -> Effects.effect(Fx.impactcloud, tile.worldx(), tile.worldy()));
        }

        Damage.damage(tile.worldx(), tile.worldy(), explosionRadius * tilesize, explosionDamage * 4);


        for(int i = 0; i < 20; i++){
            Time.run(Mathf.random(80), () -> {
                Tmp.v1.rnd(Mathf.random(40f));
                Effects.effect(Fx.explosion, Tmp.v1.x + tile.worldx(), Tmp.v1.y + tile.worldy());
            });
        }

        for(int i = 0; i < 70; i++){
            Time.run(Mathf.random(90), () -> {
                Tmp.v1.rnd(Mathf.random(120f));
                Effects.effect(Fx.impactsmoke, Tmp.v1.x + tile.worldx(), Tmp.v1.y + tile.worldy());
            });
        }
    }

    public static class FusionReactorEntity extends GeneratorEntity{
        public float warmup;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(warmup);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            warmup = stream.readFloat();
        }
    }
}
