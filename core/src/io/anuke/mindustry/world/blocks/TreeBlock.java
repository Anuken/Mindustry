package io.anuke.mindustry.world.blocks;

import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class TreeBlock extends Block{
    static final float shadowOffset = 5f;

    public TreeBlock(String name){
        super(name);
        solid = true;
        layer = Layer.power;
        expanded = true;
    }

    @Override
    public void drawShadow(Tile tile){
        Draw.rect(region, tile.drawx() - shadowOffset, tile.drawy() - shadowOffset);
    }

    @Override
    public void drawLayer(Tile tile){
        Draw.rect(region, tile.drawx(), tile.drawy());
    }
}
