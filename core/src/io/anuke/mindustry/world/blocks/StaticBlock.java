package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.graphics.CacheLayer;
import io.anuke.mindustry.world.Block;

public class StaticBlock extends Block{

    public StaticBlock(String name){
        super(name);
        cacheLayer = CacheLayer.walls;
    }

}
