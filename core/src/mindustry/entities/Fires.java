package mindustry.entities;

import arc.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Fires{
    private static final float baseLifetime = 1000f;

    /** Start a fire on the tile. If there already is a fire there, refreshes its lifetime. */
    public static void create(Tile tile){
        if(net.client() || tile == null || !state.rules.fire || !state.rules.hasEnv(Env.oxygen)) return; //not clientside.

        Fire fire = get(tile);

        if(fire == null){
            fire = Fire.create();
            fire.tile = tile;
            fire.lifetime = baseLifetime;
            fire.set(tile.worldx(), tile.worldy());
            fire.add();
            set(tile, fire);
        }else{
            fire.lifetime = baseLifetime;
            fire.time = 0f;
        }
    }

    public static @Nullable Fire get(Tile tile){
        return tile == null ? null : world.tiles.getFire(tile.array());
    }

    public static @Nullable Fire get(int x, int y){
        return Structs.inBounds(x, y, world.width(), world.height()) ? world.tiles.getFire(world.packArray(x, y)) : null;
    }

    private static void set(Tile tile, Fire fire){
        world.tiles.setFire(tile.array(), fire);
    }

    public static boolean has(int x, int y){
        if(!Structs.inBounds(x, y, world.width(), world.height())){
            return false;
        }
        Fire fire = get(x, y);
        return fire != null && fire.isAdded() && fire.fin() < 1f && fire.tile != null && fire.tile.x == x && fire.tile.y == y;
    }

    /**
     * Attempts to extinguish a fire by shortening its life. If there is no fire here, does nothing.
     */
    public static void extinguish(Tile tile, float intensity){
        if(tile != null){
            Fire fire = get(tile);
            if(fire != null){
                fire.time += intensity * Time.delta;
                Fx.steam.at(fire);
                if(fire.time >= fire.lifetime){
                    Events.fire(Trigger.fireExtinguish);
                }
            }
        }
    }

    public static void remove(Tile tile){
        if(tile != null){
            set(tile, null);
        }
    }

    public static void register(Fire fire){
        if(fire.tile != null){
            set(fire.tile, fire);
        }
    }
}
