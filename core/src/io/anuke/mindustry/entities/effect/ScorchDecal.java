package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class ScorchDecal extends Decal{
    private static final int scorches = 5;
    private static final TextureRegion[] regions = new TextureRegion[scorches];

    public static void create(float x, float y){
        if(regions[0] == null){
            for(int i = 0; i < regions.length; i++){
                regions[i] = Draw.region("scorch" + (i + 1));
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

        for(int i = 0; i < 5; i++){
            TextureRegion region = regions[Mathf.randomSeed(id - i, 0, scorches - 1)];
            float rotation = Mathf.randomSeed(id + i, 0, 360);
            float space = 1.5f + Mathf.randomSeed(id + i + 1, 0, 20) / 10f;
            Draw.grect(region, x + Angles.trnsx(rotation, space), y + Angles.trnsy(rotation, space), rotation - 90);
        }
    }
}
