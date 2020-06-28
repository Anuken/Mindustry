package mindustry.entities;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class Puddles{
    private static final IntMap<Puddle> map = new IntMap<>();

    public static final float maxLiquid = 70f;

    /** Deposists a Puddle between tile and source. */
    public static void deposit(Tile tile, Tile source, Liquid liquid, float amount){
        deposit(tile, source, liquid, amount, 0);
    }

    /** Deposists a Puddle at a tile. */
    public static void deposit(Tile tile, Liquid liquid, float amount){
        deposit(tile, tile, liquid, amount, 0);
    }

    /** Returns the Puddle on the specified tile. May return null. */
    public static Puddle get(Tile tile){
        return map.get(tile.pos());
    }

    public static void deposit(Tile tile, Tile source, Liquid liquid, float amount, int generation){
        if(tile == null) return;

        if(tile.floor().isLiquid && !canStayOn(liquid, tile.floor().liquidDrop)){
            reactPuddle(tile.floor().liquidDrop, liquid, amount, tile,
            (tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);

            Puddle p = map.get(tile.pos());

            if(generation == 0 && p != null && p.lastRipple() <= Time.time() - 40f){
                Fx.ripple.at((tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f, 1f, tile.floor().liquidDrop.color);
                p.lastRipple(Time.time());
            }
            return;
        }

        Puddle p = map.get(tile.pos());
        if(p == null){
            Puddle puddle = Puddle.create();
            puddle.tile(tile);
            puddle.liquid(liquid);
            puddle.amount(amount);
            puddle.generation(generation);
            puddle.set((tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f);
            puddle.add();
            map.put(tile.pos(), puddle);
        }else if(p.liquid() == liquid){
            p.accepting(Math.max(amount, p.accepting()));

            if(generation == 0 && p.lastRipple() <= Time.time() - 40f && p.amount() >= maxLiquid / 2f){
                Fx.ripple.at((tile.worldx() + source.worldx()) / 2f, (tile.worldy() + source.worldy()) / 2f, 1f, p.liquid().color);
                p.lastRipple(Time.time());
            }
        }else{
            p.amount(p.amount() + reactPuddle(p.liquid(), liquid, amount, p.tile(), (p.x() + source.worldx())/2f, (p.y() + source.worldy())/2f));
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
        if((dest.flammability > 0.3f && liquid.temperature > 0.7f) ||
        (liquid.flammability > 0.3f && dest.temperature > 0.7f)){ //flammable liquid + hot liquid
            Fires.create(tile);
            if(Mathf.chance(0.006 * amount)){
                Call.createBullet(Bullets.fireball, Team.derelict, x, y, Mathf.random(360f), -1f, 1f, 1f);
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
        return 0f;
    }

    /**
     * Returns whether the first liquid can 'stay' on the second one.
     * Currently, the only place where this can happen is oil on water.
     */
    private static boolean canStayOn(Liquid liquid, Liquid other){
        return liquid == Liquids.oil && other == Liquids.water;
    }
}
