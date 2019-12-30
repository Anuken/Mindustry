package mindustry.maps.generators;

import mindustry.game.*;
import mindustry.world.*;

public abstract class Generator{
    public int width, height;
    protected Schematic loadout;

    public Generator(int width, int height){
        this.width = width;
        this.height = height;
    }

    public Generator(){
    }

    public void init(Schematic loadout){
        this.loadout = loadout;
    }

    public abstract void generate(Tile[][] tiles);
}
