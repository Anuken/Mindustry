package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Blending;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import java.io.*;

import static io.anuke.mindustry.Vars.tilesize;

public class ImpactReactor extends PowerGenerator{
    protected int timerUse = timers++;

    protected int plasmas = 4;
    protected float warmupSpeed = 0.001f;
    protected float itemDuration = 60f;
    protected int explosionRadius = 50;
    protected int explosionDamage = 2000;

    protected Color plasma1 = Color.valueOf("ffd06b"), plasma2 = Color.valueOf("ff361b");
    protected Color ind1 = Color.valueOf("858585"), ind2 = Color.valueOf("fea080");
    protected int bottomRegion, topRegion, lightRegion;
    protected int[] plasmaRegions;

    public ImpactReactor(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        liquidCapacity = 30f;
        hasItems = true;
        outputsPower = consumesPower = true;

        bottomRegion = reg("-bottom");
        topRegion = reg("-top");
        lightRegion = reg("-light");
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
        FusionReactorEntity entity = tile.entity();

        if(entity.cons.valid() && entity.power.satisfaction >= 0.99f){
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, warmupSpeed);
            if(Mathf.isEqual(entity.warmup, 1f, 0.001f)){
                entity.warmup = 1f;
            }

            if(entity.timer.get(timerUse, itemDuration)){
                entity.cons.trigger();
            }
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.01f);
        }

        entity.productionEfficiency = Mathf.pow(entity.warmup, 5f);
    }

    @Override
    public void draw(Tile tile){
        FusionReactorEntity entity = tile.entity();

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

        Draw.rect(reg(topRegion), tile.drawx(), tile.drawy());

        Draw.color(ind1, ind2, entity.warmup + Mathf.absin(entity.productionEfficiency, 3f, entity.warmup * 0.5f));
        Draw.rect(reg(lightRegion), tile.drawx(), tile.drawy());

        Draw.color();
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-bottom"), Core.atlas.find(name), Core.atlas.find(name + "-top")};
    }

    @Override
    public TileEntity newEntity(){
        return new FusionReactorEntity();
    }

    @Override
    public void onDestroyed(Tile tile){
        super.onDestroyed(tile);

        FusionReactorEntity entity = tile.entity();

        if(entity.warmup < 0.4f) return;

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
