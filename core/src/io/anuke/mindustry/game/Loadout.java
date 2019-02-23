package io.anuke.mindustry.game;

import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public abstract class Loadout extends Content{
    public final Block core;

    Loadout(Block core){
        this.core = core;
    }

    public abstract void setup(Tile tile);

    @Override
    public ContentType getContentType(){
        return ContentType.loadout;
    }
}
