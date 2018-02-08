package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Block;

public class Rock extends Block {

    public Rock(String name) {
        super(name);
        shadow = name+"shadow";
        breakable = true;
        breaktime = 10;
        alwaysReplace = true;
    }
}
