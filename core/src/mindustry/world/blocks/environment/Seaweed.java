package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.world.*;

public class Seaweed extends Prop{

    public Seaweed(String name){
        super(name);
    }

    @Override
    public void drawBase(Tile tile){
        var region = variants > 0 ? variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))] : this.region;

        float
        x = tile.worldx(), y = tile.worldy(),
        rotmag = 3f, rotscl = 0.5f,
        rot = Mathf.randomSeedRange(tile.pos(), 20f) - 45 + Mathf.sin(Time.time + x, 50f * rotscl, 0.5f * rotmag) + Mathf.sin(Time.time - y, 65f * rotscl, 0.9f* rotmag) + Mathf.sin(Time.time + y - x, 85f * rotscl, 0.9f* rotmag),
        w = region.width * Draw.scl, h = region.height * Draw.scl,
        scl = 30f, mag = 0.3f;

        Draw.rectv(region, x, y, w, h, rot, vec -> vec.add(
        Mathf.sin(vec.y*3 + Time.time, scl, mag) + Mathf.sin(vec.x*3 - Time.time, 70, 0.8f),
        Mathf.cos(vec.x*3 + Time.time + 8, scl + 6f, mag * 1.1f) + Mathf.sin(vec.y*3 - Time.time, 50, 0.2f)
        ));
    }
}
