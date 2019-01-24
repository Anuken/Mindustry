package io.anuke.mindustry.world.blocks;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.graphics.CacheLayer;

public class StaticWall extends Rock{
    TextureRegion[][] regions;

    public StaticWall(String name){
        super(name);
        breakable = alwaysReplace = false;
        solid = true;
        cacheLayer = CacheLayer.walls;
    }

    @Override
    public void load(){
        super.load();
        int size = (int)(8 / Draw.scl);
        regions = Core.atlas.find("mountains-tiles").split(size, size);
    }
}
