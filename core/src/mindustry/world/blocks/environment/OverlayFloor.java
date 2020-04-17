package mindustry.world.blocks.environment;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import mindustry.world.Tile;

/**A type of floor that is overlaid on top of over floors.*/
public class OverlayFloor extends Floor{

    public OverlayFloor(String name){
        super(name);
    }

    @Override
    public void drawBase(Tile tile){
        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());
    }
}
