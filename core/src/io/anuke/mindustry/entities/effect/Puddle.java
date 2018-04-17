package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class Puddle extends Entity {
    private static final IntMap<Puddle> map = new IntMap<>();
    private static final float maxLiquid = 70f;
    private static final int maxGeneration = 2;
    private static final Color tmp = new Color();
    private static final Rectangle rect = new Rectangle();

    private Tile tile;
    private Liquid liquid;
    private float amount;
    private int generation;
    private float accepting;

    /**Deposists a puddle between tile and source.*/
    public static void deposit(Tile tile, Tile source, Liquid liquid, float amount){
        deposit(tile, source, liquid, amount, 0);
    }

    /**Deposists a puddle at a tile.*/
    public static void deposit(Tile tile, Liquid liquid, float amount){
        deposit(tile, tile, liquid, amount, 0);
    }

    /**Returns the puddle on the specified tile. May return null.*/
    public static Puddle getPuddle(Tile tile){
        return map.get(tile.packedPosition());
    }

    private static void deposit(Tile tile, Tile source, Liquid liquid, float amount, int generation){
        Puddle p = map.get(tile.packedPosition());
        if(p == null){
            Puddle puddle = new Puddle(tile, source, liquid, amount, generation).add();
            map.put(tile.packedPosition(), puddle);
        }else if(p.liquid == liquid){
            p.accepting = Math.max(amount, p.accepting);

            if(generation == 0 && Timers.get(p, "ripple", 50) && p.amount >= maxLiquid/2f){
                Effects.effect(BlockFx.ripple, p.liquid.color, (tile.worldx() + source.worldx())/2f, (tile.worldy() + source.worldy())/2f);
            }
        }else{
            reactPuddle(p, liquid, amount);
        }
    }

    private static void reactPuddle(Puddle p, Liquid liquid, float amount){
        if((p.liquid.flammability > 0.3f && liquid.temperature > 0.7f) ||
                liquid.flammability > 0.3f && p.liquid.temperature > 0.7f){ //flammable liquid + hot liquid
            Fire.create(p.tile);
            if(Mathf.chance(0.006 * amount)){
                new Fireball(p.x, p.y, p.liquid.flameColor, Mathf.random(360f)).add();
            }
        }else if(p.liquid.temperature > 0.7f && liquid.temperature < 0.55f){ //cold liquid poured onto hot puddle
            if(Mathf.chance(0.5f * amount)){
                Effects.effect(EnvironmentFx.steam, p.x, p.y);
            }
            p.amount -= 0.1f * amount;
        }else if(liquid.temperature > 0.7f && p.liquid.temperature < 0.55f){ //hot liquid poured onto cold puddle
            if(Mathf.chance(0.8f * amount)){
                Effects.effect(EnvironmentFx.steam, p.x, p.y);
            }
            p.amount -= 0.4f * amount;
        }
    }

    private Puddle(Tile tile, Tile source, Liquid liquid, float amount, int generation) {
        this.tile = tile;
        this.liquid = liquid;
        this.amount = amount;
        this.generation = generation;
        set((tile.worldx() + source.worldx())/2f, (tile.worldy() + source.worldy())/2f);
    }

    public float getFlammability(){
        return liquid.flammability * amount;
    }

    @Override
    public void update() {
        float addSpeed = accepting > 0 ? 3f : 0f;

        amount -= Timers.delta() * (1f - liquid.viscosity) /(5f+addSpeed);

        amount += accepting;
        accepting = 0f;

        if(amount >= maxLiquid/1.5f && generation < maxGeneration){
            float deposited = Math.min((amount - maxLiquid/1.5f)/4f, 0.3f) * Timers.delta();
            for(GridPoint2 point : Geometry.d4){
                Tile other = world.tile(tile.x + point.x, tile.y + point.y);
                if(other.block() == Blocks.air){
                    deposit(other, tile, liquid, deposited, generation + 1);
                    amount -= deposited/4f;
                }
            }
        }

        if(amount >= maxLiquid/2f && Timers.get(this, "update", 20)){
            Units.getNearby(rect.setSize(Mathf.clamp(amount/(maxLiquid/1.5f))*10f).setCenter(tile.worldx(), tile.worldy()), unit -> {
                Rectangle o = unit.hitbox.getRect(unit.x, unit.y);
                if(!rect.overlaps(o)) return;

                unit.applyEffect(liquid.effect, 0.5f);
                if(unit.velocity.len() > 0.4) {
                    Effects.effect(BlockFx.ripple, liquid.color, unit.x, unit.y);
                }
            });

            if(liquid.temperature > 0.7f && tile.entity != null && Mathf.chance(0.3 * Timers.delta())){
                Fire.create(tile);
            }
        }

        amount = Mathf.clamp(amount, 0, maxLiquid);

        if(amount <= 0f){
            remove();
        }
    }

    @Override
    public void draw() {
        float f = Mathf.clamp(amount/(maxLiquid/1.5f));

        Draw.color(Hue.shift(tmp.set(liquid.color), 2, -0.05f));
        Fill.circle(x, y, f * 8f);
        Angles.randLenVectors(id, 3, f * 6f, (ex, ey) -> {
            Fill.circle(x + ex, y + ey, f * 5f);
        });
        Draw.color();
    }

    @Override
    public void removed() {
        map.remove(tile.packedPosition());
    }

    @Override
    public Puddle add() {
        return add(Vars.groundEffectGroup);
    }
}
