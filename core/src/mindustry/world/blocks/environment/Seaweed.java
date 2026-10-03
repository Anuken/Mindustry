package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.world.*;

public class Seaweed extends Prop{

    public Seaweed(String name){
        super(name);

        obstructsLight = false;
    }

    @Override
    public void drawBase(Tile tile){
        var region = variants > 0 ? variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))] : this.region;

        float
        x = tile.worldx(), y = tile.worldy(),
        rotmag = 3f, rotscl = 0.5f,
        rot = Mathf.randomSeedRange(tile.pos(), 20f) - 45 + Mathf.sin(Vars.state.time + x, 50f * rotscl, 0.5f * rotmag) + Mathf.sin(Vars.state.time - y, 65f * rotscl, 0.9f* rotmag) + Mathf.sin(Vars.state.time + y - x, 85f * rotscl, 0.9f* rotmag),
        w = region.width * region.scl(), h = region.height * region.scl(),
        scl = 30f, mag = 0.3f;

        Draw.rectv(region, x, y, w, h, rot, vec -> vec.add(
        Mathf.sin(vec.y*3 + Vars.state.time, scl, mag) + Mathf.sin(vec.x*3 - Vars.state.time, 70, 0.8f),
        Mathf.cos(vec.x*3 + Vars.state.time + 8, scl + 6f, mag * 1.1f) + Mathf.sin(vec.y*3 - Vars.state.time, 50, 0.2f)
        ));
    }
}
