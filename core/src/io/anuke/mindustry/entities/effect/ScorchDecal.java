package io.anuke.mindustry.entities.effect;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.headless;
import static io.anuke.mindustry.Vars.world;

public class ScorchDecal extends Decal{
    private static final int scorches = 5;
    private static final TextureRegion[] regions = new TextureRegion[scorches];

    public static void create(float x, float y){
        if(headless) return;

        if(regions[0] == null || regions[0].getTexture().isDisposed()){
            for(int i = 0; i < regions.length; i++){
                regions[i] = Core.atlas.find("scorch" + (i + 1));
            }
        }

        Tile tile = world.tileWorld(x, y);

        if(tile == null || tile.floor().liquidDrop != null) return;

        ScorchDecal decal = new ScorchDecal();
        decal.set(x, y);
        decal.add();
    }

    @Override
    public void drawDecal(){
        for(int i = 0; i < 3; i++){
            TextureRegion region = regions[Mathf.randomSeed(id - i, 0, scorches - 1)];
            float rotation = Mathf.randomSeed(id + i, 0, 360);
            float space = 1.5f + Mathf.randomSeed(id + i + 1, 0, 20) / 10f;
            Draw.rect(region,
            x + Angles.trnsx(rotation, space),
            y + Angles.trnsy(rotation, space) + region.getHeight() / 2f * Draw.scl,
            region.getWidth() * Draw.scl,
            region.getHeight() * Draw.scl,
            region.getWidth() / 2f * Draw.scl, 0, rotation - 90);
        }
    }
}
