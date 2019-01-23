package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.graphics.CacheLayer;

public class StaticWall extends Rock{

    public StaticWall(String name){
        super(name);
        breakable = alwaysReplace = false;
        solid = true;
        cacheLayer = CacheLayer.walls;
    }
}
