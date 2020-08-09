package mindustry.entities;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Fires{
    private static final float baseLifetime = 1000f;
    private static final IntMap<Fire> map = new IntMap<>();

    /** Start a fire on the tile. If there already is a file there, refreshes its lifetime. */
    public static void create(Tile tile){
        if(net.client() || tile == null) return; //not clientside.

        Fire fire = map.get(tile.pos());

        if(fire == null){
            fire = Fire.create();
            fire.tile(tile);
            fire.lifetime(baseLifetime);
            fire.set(tile.worldx(), tile.worldy());
            fire.add();
            map.put(tile.pos(), fire);
        }else{
            fire.lifetime(baseLifetime);
            fire.time(0f);
        }
    }

    public static Fire get(int x, int y){
        return map.get(Point2.pack(x, y));
    }

    public static boolean has(int x, int y){
        if(!Structs.inBounds(x, y, world.width(), world.height()) || !map.containsKey(Point2.pack(x, y))){
            return false;
        }
        Fire fire = map.get(Point2.pack(x, y));
        return fire.isAdded() && fire.fin() < 1f && fire.tile() != null && fire.tile().x == x && fire.tile().y == y;
    }

    /**
     * Attempts to extinguish a fire by shortening its life. If there is no fire here, does nothing.
     */
    public static void extinguish(Tile tile, float intensity){
        if(tile != null && map.containsKey(tile.pos())){
            Fire fire = map.get(tile.pos());
            fire.time(fire.time + intensity * Time.delta);
            Fx.steam.at(fire);
            if(fire.time >= fire.lifetime){
                Events.fire(Trigger.fireExtinguish);
            }
        }
    }

    public static void remove(Tile tile){
        map.remove(tile.pos());
    }

    public static void register(Fire fire){
        map.put(fire.tile.pos(), fire);
    }
}
