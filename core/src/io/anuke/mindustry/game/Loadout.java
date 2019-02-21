package io.anuke.mindustry.game;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public enum Loadout{
    test(Blocks.coreShard){
        @Override
        public void setup(Tile tile){

        }
    };

    public final Block core;

    Loadout(Block core){
        this.core = core;
    }

    public abstract void setup(Tile tile);
}
