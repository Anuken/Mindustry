package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;

public class NuclearReactor extends PowerGenerator{
    protected final int timerFuel = timers++;

    protected final Vector2 tr = new Vector2();

    protected Color coolColor = new Color(1, 1, 1, 0f);
    protected Color hotColor = Color.valueOf("ff9575a3");
    protected int fuelUseTime = 120; //time to consume 1 fuel
    protected float heating = 0.013f; //heating per frame
    protected float coolantPower = 0.015f; //how much heat decreases per coolant unit
    protected float smokeThreshold = 0.3f; //threshold at which block starts smoking
    protected float maxLiquidUse = 2f; //max liquid use per frame
    protected int explosionRadius = 19;
    protected int explosionDamage = 135;
    protected float flashThreshold = 0.46f; //heat threshold at which the lights start flashing

    protected TextureRegion topRegion, lightsRegion;

    public NuclearReactor(String name){
        super(name);
        itemCapacity = 30;
        liquidCapacity = 50;
        hasItems = true;
        hasLiquids = true;

        consumes.item(Items.thorium);
    }

    @Override
    public void load(){
        super.load();

        topRegion = Core.atlas.find(name + "-center");
        lightsRegion = Core.atlas.find(name + "-lights");
    }

    @Override
    public void update(Tile tile){
        NuclearReactorEntity entity = tile.entity();

        int fuel = entity.items.get(consumes.item());
        float fullness = (float) fuel / itemCapacity;
        entity.productionEfficiency = fullness;

        if(fuel > 0){
            entity.heat += fullness * heating * Math.min(entity.delta(), 4f);

            if(entity.timer.get(timerFuel, fuelUseTime)){
                entity.items.remove(consumes.item(), 1);
            }
        }

        if(entity.liquids.total() > 0){
            Liquid liquid = entity.liquids.current();

            if(liquid.temperature <= 0.5f){ //is coolant
                float pow = coolantPower * (liquid.heatCapacity + 0.5f / liquid.temperature); //heat depleted per unit of liquid
                float maxUsed = Math.min(Math.min(entity.liquids.get(liquid), entity.heat / pow), maxLiquidUse * entity.delta()); //max that can be cooled in terms of liquid
                entity.heat -= maxUsed * pow;
                entity.liquids.remove(liquid, maxUsed);
            }else{ //is heater
                float heat = coolantPower * liquid.heatCapacity / 4f; //heat created per unit of liquid
                float maxUsed = Math.min(Math.min(entity.liquids.get(liquid), (1f - entity.heat) / heat), maxLiquidUse * entity.delta()); //max liquid used
                entity.heat += maxUsed * heat;
                entity.liquids.remove(liquid, maxUsed);
            }
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
            entity.kill();
        }else{
            super.update(tile);
        }
    }

    @Override
    public void onDestroyed(Tile tile){
        super.onDestroyed(tile);

        NuclearReactorEntity entity = tile.entity();

        int fuel = entity.items.get(consumes.item());

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
        Draw.rect("white", tile.drawx(), tile.drawy(), size * tilesize, size * tilesize);

        Draw.color(entity.liquids.current().color);
        Draw.alpha(entity.liquids.currentAmount() / liquidCapacity);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());

        if(entity.heat > flashThreshold){
            float flash = 1f + ((entity.heat - flashThreshold) / (1f - flashThreshold)) * 5.4f;
            entity.flash += flash * Time.delta();
            Draw.color(Color.RED, Color.YELLOW, Mathf.absin(entity.flash, 9f, 1f));
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
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            heat = stream.readFloat();
        }
    }
}
