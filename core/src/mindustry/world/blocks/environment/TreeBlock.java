package mindustry.world.blocks.environment;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;

public class TreeBlock extends Block{

    public TreeBlock(String name){
        super(name);
        solid = true;
        expanded = true;
    }

    @Override
    public void drawBase(Tile tile){
        Draw.z(Layer.power + 1);
        Draw.rect(region, tile.worldx(), tile.worldy(), Mathf.randomSeed(tile.pos(), 0, 4) * 90);
    }
}
