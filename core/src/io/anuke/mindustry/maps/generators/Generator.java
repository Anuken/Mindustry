package io.anuke.mindustry.maps.generators;

import io.anuke.mindustry.type.Loadout;
import io.anuke.mindustry.world.Tile;

public abstract class Generator{
    public int width, height;
    protected Loadout loadout;

    public Generator(int width, int height){
        this.width = width;
        this.height = height;
    }

    public Generator(){
    }

    public void init(Loadout loadout){
        this.loadout = loadout;
    }

    public abstract void generate(Tile[][] tiles);
}
