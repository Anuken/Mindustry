package io.anuke.mindustry.entities.effect;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;

import static io.anuke.mindustry.Vars.headless;

public class RubbleDecal extends Decal{
    private static final TextureRegion[][] regions = new TextureRegion[16][0];
    private TextureRegion region;

    /**
     * Creates a rubble effect at a position. Provide a block size to use.
     */
    public static void create(float x, float y, int size){
        if(headless) return;

        if(regions[size].length == 0){
            int i = 0;
            for(; i < 2; i++){
                if(!Core.atlas.has("rubble-" + size + "-" + i)){
                    break;
                }
            }
            regions[size] = new TextureRegion[i + 1];
            for(int j = 0; j <= i; j++){
                regions[size][j] = Core.atlas.find("rubble-" + size + "-" + j);
            }
        }

        RubbleDecal decal = new RubbleDecal();
        decal.region = regions[size][Mathf.clamp(Mathf.randomSeed(decal.id, 0, 1), 0, regions[size].length - 1)];
        decal.set(x, y);
        decal.add();
    }

    @Override
    public void drawDecal(){
        Draw.rect(region, x, y, Mathf.randomSeed(id, 0, 4) * 90);
    }
}
