package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.world.*;

public class WobbleProp extends Prop{
    public float wscl = 25f, wmag = 0.4f, wtscl = 1f, wmag2 = 1f;

    public WobbleProp(String name){
        super(name);
    }

    @Override
    public void drawBase(Tile tile){
        var region = variants > 0 ? variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))] : this.region;

        Draw.rectv(region, tile.worldx(), tile.worldy(), region.width * Draw.scl, region.height * Draw.scl, 0, vec -> vec.add(
        Mathf.sin(vec.y*3 + Time.time, wscl, wmag) + Mathf.sin(vec.x*3 - Time.time, 70 * wtscl, 0.8f * wmag2),
        Mathf.cos(vec.x*3 + Time.time + 8, wscl + 6f, wmag * 1.1f) + Mathf.sin(vec.y*3 - Time.time, 50 * wtscl, 0.2f * wmag2)
        ));
    }
}
