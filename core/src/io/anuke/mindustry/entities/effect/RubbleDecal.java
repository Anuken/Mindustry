package io.anuke.mindustry.entities.effect;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;

public class RubbleDecal extends Decal{
    private int size;

    /**
     * Creates a rubble effect at a position. Provide a block size to use.
     */
    public static void create(float x, float y, int size){
        RubbleDecal decal = new RubbleDecal();
        decal.size = size;
        decal.set(x, y);
        decal.add();
    }

    @Override
    public void drawDecal(){
        String region = "rubble-" + size + "-" + Mathf.randomSeed(id, 0, 1);

        if(!Core.atlas.has(region)){
            remove();
            return;
        }

        Draw.rect(region, x, y).rot(Mathf.randomSeed(id, 0, 4) * 90);
    }
}
