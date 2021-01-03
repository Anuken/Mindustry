package mindustry.net;

import arc.struct.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class Amendments{
    public IntMap<String> builders = new IntMap<>();

//    public void read(){
//        builders.each((build, builder) -> {
//            if(build != null) build.lastAccessed = builder;
//        });
//    }
//
//    public LastAccessed write(){
//        world.tiles.eachTile(tile -> {
//            if(tile.build != null && tile.build.lastAccessed != null && !tile.build.lastAccessed.isEmpty()) builders.put(tile.build, tile.build.lastAccessed);
//        });
//
//        return this;
//    }
}
