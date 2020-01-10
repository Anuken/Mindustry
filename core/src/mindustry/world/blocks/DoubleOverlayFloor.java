package mindustry.world.blocks;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import mindustry.world.Tile;

public class DoubleOverlayFloor extends OverlayFloor{

    public DoubleOverlayFloor(String name){
        super(name);
    }

    @Override
    public void draw(Tile tile){
        Draw.colorl(0.4f);
        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy() - 0.75f);
        Draw.color();
        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());
    }
}
