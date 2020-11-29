package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.world.*;

/**A type of floor that is overlaid on top of other floors.*/
public class OverlayFloor extends Floor{

    public OverlayFloor(String name){
        super(name);
        useColor = false;
    }

    @Override
    public void drawBase(Tile tile){
        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());
    }
}
