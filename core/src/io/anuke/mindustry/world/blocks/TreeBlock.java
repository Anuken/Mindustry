package io.anuke.mindustry.world.blocks;

import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class TreeBlock extends Block{

    public TreeBlock(String name){
        super(name);
        solid = true;
        layer = Layer.power;
        //cacheLayer = CacheLayer.walls;
        expanded = true;
    }

    @Override
    public void drawLayer(Tile tile){
        Draw.colorl(1f - tile.getRotation() / 4f);
        Draw.rect(region, tile.drawx(), tile.drawy(), Mathf.randomSeed(tile.pos(), 0, 4) * 90);
        Draw.color();
    }
}
