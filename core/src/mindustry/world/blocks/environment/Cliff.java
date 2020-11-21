package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class Cliff extends Block{
    public float size = 11f;
    public @Load(value = "cliffmask#", length = 256) TextureRegion[] cliffs;

    public Cliff(String name){
        super(name);
        breakable = alwaysReplace = false;
        solid = true;
        cacheLayer = CacheLayer.walls;
        fillsTile = false;
        hasShadow = false;
    }

    @Override
    public void drawBase(Tile tile){
        Draw.color(Tmp.c1.set(tile.floor().mapColor).mul(1.6f));
        Draw.rect(cliffs[tile.data & 0xff], tile.worldx(), tile.worldy());
        Draw.color();
    }

    @Override
    public int minimapColor(Tile tile){
        return Tmp.c1.set(tile.floor().mapColor).mul(1.2f).rgba();
    }
}
