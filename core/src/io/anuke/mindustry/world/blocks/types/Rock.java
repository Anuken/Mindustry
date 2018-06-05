package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Block;

public class Rock extends Block {

    public Rock(String name) {
        super(name);
        varyShadow = true;
        breakable = true;
        alwaysReplace = true;
    }
}
