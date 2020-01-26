package mindustry.entities.effect;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;

import static mindustry.Vars.headless;

public class RubbleDecal extends Decal{
    private TextureRegion region;

    /** Creates a rubble effect at a position. Provide a block size to use. */
    public static void create(float x, float y, int size){
        if(headless) return;

        RubbleDecal decal = new RubbleDecal();
        decal.region = Core.atlas.find("rubble-" + size + "-" + Mathf.randomSeed(decal.id, 0, 1));

        if(!Core.atlas.isFound(decal.region)){
            return;
        }

        decal.set(x, y);
        decal.add();
    }

    @Override
    public float lifetime(){
        return 8200f;
    }

    @Override
    public void drawDecal(){
        if(!Core.atlas.isFound(region)){
            remove();
            return;
        }
        Draw.rect(region, x, y, Mathf.randomSeed(id, 0, 4) * 90);
    }

    @Override
    public float drawSize(){
        return region.getWidth() * 3f;
    }
}
