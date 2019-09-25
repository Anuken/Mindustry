package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.*;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.*;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import java.io.*;

import static io.anuke.mindustry.Vars.tilesize;

public class NuclearReactor extends PowerGenerator{
    protected final int timerFuel = timers++;

    protected final Vector2 tr = new Vector2();

    protected Color coolColor = new Color(1, 1, 1, 0f);
    protected Color hotColor = Color.valueOf("ff9575a3");
    protected float itemDuration = 120; //time to consume 1 fuel
    protected float heating = 0.01f; //heating per frame * fullness
    protected float smokeThreshold = 0.3f; //threshold at which block starts smoking
    protected int explosionRadius = 40;
    protected int explosionDamage = 1350;
    protected float flashThreshold = 0.46f; //heat threshold at which the lights start flashing
    protected float coolantPower = 0.5f;

    protected TextureRegion topRegion, lightsRegion;

    public NuclearReactor(String name){
        super(name);
        itemCapacity = 30;
        liquidCapacity = 30;
        hasItems = true;
        hasLiquids = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        if(hasItems){
            stats.add(BlockStat.productionTime, itemDuration / 60f, StatUnit.seconds);
        }
    }

    @Override
    public void load(){
        super.load();

        topRegion = Core.atlas.find(name + "-center");
        lightsRegion = Core.atlas.find(name + "-lights");
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("heat", entity -> new Bar("bar.heat", Pal.lightOrange, () -> ((NuclearReactorEntity)entity).heat));
    }

    @Override
    public void update(Tile tile){
        NuclearReactorEntity entity = tile.entity();

        ConsumeLiquid cliquid = consumes.get(ConsumeType.liquid);
        Item item = consumes.<ConsumeItems>get(ConsumeType.item).items[0].item;

        int fuel = entity.items.get(item);
        float fullness = (float)fuel / itemCapacity;
        entity.productionEfficiency = fullness;

        if(fuel > 0){
            entity.heat += fullness * heating * Math.min(entity.delta(), 4f);

            if(entity.timer.get(timerFuel, itemDuration / entity.timeScale)){
                entity.cons.trigger();
            }
        }

        Liquid liquid = cliquid.liquid;
        float liquidAmount = cliquid.amount;

        if(entity.heat > 0){
            float maxUsed = Math.min(Math.min(entity.liquids.get(liquid), entity.heat / coolantPower), liquidAmount * entity.delta());
            entity.heat -= maxUsed * coolantPower;
            entity.liquids.remove(liquid, maxUsed);
        }

        if(entity.heat > smokeThreshold){
            float smoke = 1.0f + (entity.heat - smokeThreshold) / (1f - smokeThreshold); //ranges from 1.0 to 2.0
            if(Mathf.chance(smoke / 20.0 * entity.delta())){
                Effects.effect(Fx.reactorsmoke, tile.worldx() + Mathf.range(size * tilesize / 2f),
                tile.worldy() + Mathf.random(size * tilesize / 2f));
            }
        }

        entity.heat = Mathf.clamp(entity.heat);

        if(entity.heat >= 0.999f){
            Events.fire(Trigger.thoriumReactorOverheat);
            entity.kill();
        }
    }

    @Override
    public void onDestroyed(Tile tile){
        super.onDestroyed(tile);

        Sounds.explosionbig.at(tile);

        NuclearReactorEntity entity = tile.entity();

        int fuel = entity.items.get(consumes.<ConsumeItems>get(ConsumeType.item).items[0].item);

        if(fuel < 5 && entity.heat < 0.5f) return;

        Effects.shake(6f, 16f, tile.worldx(), tile.worldy());
        Effects.effect(Fx.nuclearShockwave, tile.worldx(), tile.worldy());
        for(int i = 0; i < 6; i++){
            Time.run(Mathf.random(40), () -> Effects.effect(Fx.nuclearcloud, tile.worldx(), tile.worldy()));
        }

        Damage.damage(tile.worldx(), tile.worldy(), explosionRadius * tilesize, explosionDamage * 4);

        for(int i = 0; i < 20; i++){
            Time.run(Mathf.random(50), () -> {
                tr.rnd(Mathf.random(40f));
                Effects.effect(Fx.explosion, tr.x + tile.worldx(), tr.y + tile.worldy());
            });
        }

        for(int i = 0; i < 70; i++){
            Time.run(Mathf.random(80), () -> {
                tr.rnd(Mathf.random(120f));
                Effects.effect(Fx.nuclearsmoke, tr.x + tile.worldx(), tr.y + tile.worldy());
            });
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        NuclearReactorEntity entity = tile.entity();

        Draw.color(coolColor, hotColor, entity.heat);
        Fill.rect(tile.drawx(), tile.drawy(), size * tilesize, size * tilesize);

        Draw.color(entity.liquids.current().color);
        Draw.alpha(entity.liquids.currentAmount() / liquidCapacity);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());

        if(entity.heat > flashThreshold){
            float flash = 1f + ((entity.heat - flashThreshold) / (1f - flashThreshold)) * 5.4f;
            entity.flash += flash * Time.delta();
            Draw.color(Color.red, Color.yellow, Mathf.absin(entity.flash, 9f, 1f));
            Draw.alpha(0.6f);
            Draw.rect(lightsRegion, tile.drawx(), tile.drawy());
        }

        Draw.reset();
    }

    @Override
    public TileEntity newEntity(){
        return new NuclearReactorEntity();
    }

    public static class NuclearReactorEntity extends GeneratorEntity{
        public float heat;
        public float flash;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            heat = stream.readFloat();
        }
    }
}
