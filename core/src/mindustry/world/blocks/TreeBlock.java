package mindustry.world.blocks;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;

public class TreeBlock extends Block{

    public TreeBlock(String name){
        super(name);
        solid = true;
        layer = Layer.power;
        expanded = true;
    }

    @Override
    public void draw(Tile tile){}

    @Override
    public void drawLayer(Tile tile){
        Draw.rect(region, tile.drawx(), tile.drawy(), Mathf.randomSeed(tile.pos(), 0, 4) * 90);
    }
}
