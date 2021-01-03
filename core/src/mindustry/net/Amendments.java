package mindustry.net;

import arc.struct.*;
import arc.util.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Amendments{
    public IntMap<String> interactions = new IntMap<>();

    public static Amendments create(){
        Amendments amendments = new Amendments();

        world.tiles.eachTile(tile -> {
            if(tile.build != null && tile.build.lastAccessed != null && !tile.build.lastAccessed.isEmpty()) amendments.interactions.put(tile.build.pos(), tile.build.lastAccessed);
        });

        return amendments;
    }

    public void implement(){
        for(var entry : interactions.entries()){
            Tile tile = world.tile(entry.key);
            if(tile == null || tile.build == null){
                Log.warn("Missing entity at @. Skipping last accessed.", tile);
            }else{
                tile.build.lastAccessed = entry.value;
            }
        }
    }
}
