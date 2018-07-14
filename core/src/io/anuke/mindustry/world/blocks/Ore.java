package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.content.blocks.Blocks;

public class Ore extends Floor{

    public Ore(String name){
        super(name);
        blends = block -> block != this && block != Blocks.stone;
    }

}
