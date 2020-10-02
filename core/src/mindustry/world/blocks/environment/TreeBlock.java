package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;

public class TreeBlock extends Block{
    public @Load("@-shadow") TextureRegion shadow;
    public float shadowOffset = -4f;

    public TreeBlock(String name){
        super(name);
        solid = true;
        expanded = true;
    }

    @Override
    public void drawBase(Tile tile){

        float x = tile.worldx(), y = tile.worldy();
        float rot = Mathf.randomSeed(tile.pos(), 0, 4) * 90 + Mathf.sin(Time.time() + x, 50f, 0.5f) + Mathf.sin(Time.time() - y, 65f, 0.9f) + Mathf.sin(Time.time() + y - x, 85f, 0.9f);
        float w = region.width * Draw.scl, h = region.height * Draw.scl;
        float scl = 30f, mag = 0.2f;

        if(shadow.found()){
            Draw.z(Layer.power - 1);
            Draw.rect(shadow, tile.worldx() + shadowOffset, tile.worldy() + shadowOffset, rot);
        }

        Draw.z(Layer.power + 1);
        Draw.rectv(region, x, y, w, h, rot, vec -> {
            vec.add(
            Mathf.sin(vec.y*3 + Time.time(), scl, mag) + Mathf.sin(vec.x*3 - Time.time(), 70, 0.8f),
            Mathf.cos(vec.x*3 + Time.time() + 8, scl + 6f, mag * 1.1f) + Mathf.sin(vec.y*3 - Time.time(), 50, 0.2f)
            );
        });
    }

    void tweak(Vec2 vec){

    }
}
