package mindustry.entities;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class Puddles{
    private static final IntMap<Puddle> map = new IntMap<>();

    public static final float maxLiquid = 70f;

    /** Deposits a Puddle between tile and source. */
    public static void deposit(Tile tile, Tile source, Liquid liquid, float amount){
        deposit(tile, source, liquid, amount, true);
    }

    /** Deposits a Puddle at a tile. */
    public static void deposit(Tile tile, Liquid liquid, float amount){
        deposit(tile, tile, liquid, amount, true);
    }

    /** Returns the Puddle on the specified tile. May return null. */
    public static Puddle get(Tile tile){
        return map.get(tile.pos());
    }

    public static void deposit(Tile tile, Tile source, Liquid liquid, float amount, boolean initial){
        deposit(tile, source, liquid, amount, initial, false);
    }

    public static void deposit(Tile tile, Tile source, Liquid liquid, float amount, boolean initial, boolean cap){
        if(tile == null) return;

        float ax = (tile.worldx() + source.worldx()) / 2f, ay = (tile.worldy() + source.worldy()) / 2f;

        if(liquid.willBoil()){
            if(Mathf.chanceDelta(0.16f)){
                liquid.vaporEffect.at(ax, ay, liquid.gasColor);
            }
            return;
        }

        if(Vars.state.rules.hasEnv(Env.space)){
            if(Mathf.chanceDelta(0.11f) && tile != source){
                Bullets.spaceLiquid.create(null, source.team(), ax, ay, source.angleTo(tile) + Mathf.range(50f), -1f, Mathf.random(0f, 0.2f), Mathf.random(0.6f, 1f), liquid);
            }
            return;
        }

        if(tile.floor().isLiquid && !canStayOn(liquid, tile.floor().liquidDrop)){
            reactPuddle(tile.floor().liquidDrop, liquid, amount, tile, ax, ay);

            Puddle p = map.get(tile.pos());

            if(initial && p != null && p.lastRipple <= Time.time - 40f){
                Fx.ripple.at(ax, ay, 1f, tile.floor().liquidDrop.color);
                p.lastRipple = Time.time;
            }
            return;
        }

        if(tile.floor().solid) return;

        Puddle p = map.get(tile.pos());
        if(p == null || p.liquid == null){
            Puddle puddle = Puddle.create();
            puddle.tile = tile;
            puddle.liquid = liquid;
            puddle.amount = amount;
            puddle.set(ax, ay);
            map.put(tile.pos(), puddle);
            puddle.add();
        }else if(p.liquid == liquid){
            p.accepting = Math.max(amount, p.accepting);

            if(initial && p.lastRipple <= Time.time - 40f && p.amount >= maxLiquid / 2f){
                Fx.ripple.at(ax, ay, 1f, p.liquid.color);
                p.lastRipple = Time.time;
            }
        }else{
            float added = reactPuddle(p.liquid, liquid, amount, p.tile, (p.x + source.worldx())/2f, (p.y + source.worldy())/2f);

            if(cap){
                added = Mathf.clamp(maxLiquid - p.amount, 0f, added);
            }

            p.amount += added;
        }
    }

    public static void remove(Tile tile){
        if(tile == null) return;

        map.remove(tile.pos());
    }

    public static void register(Puddle puddle){
        map.put(puddle.tile().pos(), puddle);
    }

    /** Reacts two liquids together at a location. */
    private static float reactPuddle(Liquid dest, Liquid liquid, float amount, Tile tile, float x, float y){
        if(dest == null) return 0f;

        if((dest.flammability > 0.3f && liquid.temperature > 0.7f) ||
        (liquid.flammability > 0.3f && dest.temperature > 0.7f)){ //flammable liquid + hot liquid
            Fires.create(tile);
            if(Mathf.chance(0.006 * amount)){
                Bullets.fireball.createNet(Team.derelict, x, y, Mathf.random(360f), -1f, 1f, 1f);
            }
        }else if(dest.temperature > 0.7f && liquid.temperature < 0.55f){ //cold liquid poured onto hot Puddle
            if(Mathf.chance(0.5f * amount)){
                Fx.steam.at(x, y);
            }
            return -0.1f * amount;
        }else if(liquid.temperature > 0.7f && dest.temperature < 0.55f){ //hot liquid poured onto cold Puddle
            if(Mathf.chance(0.8f * amount)){
                Fx.steam.at(x, y);
            }
            return -0.4f * amount;
        }
        return dest.react(liquid, amount, tile, x, y);
    }

    /**
     * Returns whether the first liquid can 'stay' on the second one.
     */
    private static boolean canStayOn(Liquid liquid, Liquid other){
        return liquid.canStayOn.contains(other);
    }
}
