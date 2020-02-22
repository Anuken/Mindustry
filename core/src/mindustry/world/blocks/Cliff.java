package mindustry.world.blocks;

import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class Cliff extends Block{

    public Cliff(String name){
        super(name);
        breakable = alwaysReplace = false;
        solid = true;
        cacheLayer = CacheLayer.walls;
        fillsTile = false;
        hasShadow = false;
    }

    @Override
    public void draw(Tile tile){
        int r = tile.rotation();
        for(int i = 0; i < 4; i++){
            if((r & (1 << i)) != 0){
                Draw.color(Tmp.c1.set(tile.floor().color).mul(1.3f + (i >= 2 ? -0.4f : 0.3f)));
                Draw.rect(region, tile.worldx(), tile.worldy(), i * 90f);
            }
        }

        Draw.color();
    }
}
