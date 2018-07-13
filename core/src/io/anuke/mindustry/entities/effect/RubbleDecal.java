package io.anuke.mindustry.entities.effect;

import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

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

        if(!Draw.hasRegion(region)){
            remove();
            return;
        }

        Draw.rect(region, x, y, Mathf.randomSeed(id, 0, 4) * 90);
    }
}
