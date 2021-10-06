package mindustry.entities.abilities;

import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class LiquidExplodeAbility extends Ability{
    public Liquid liquid = Liquids.water;
    public float amount = 120f;
    public float radAmountScale = 5f, radScale = 1f;
    public float noiseMag = 6.5f, noiseScl = 5f;

    @Override
    public void death(Unit unit){
        //TODO what if noise is radial, so it looks like a splat?
        int tx = unit.tileX(), ty = unit.tileY();
        int rad = Math.max((int)(unit.hitSize / tilesize * radScale), 1);
        float realNoise = unit.hitSize / noiseMag;
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                if(x*x + y*y <= rad*rad - Simplex.noise2d(0, 2, 0.5f, 1f / noiseScl, x + tx, y + ty) * realNoise * realNoise){
                    float scaling = (1f - Mathf.dst(x, y) / rad) * radAmountScale;

                    Tile tile = world.tile(tx + x, ty + y);
                    if(tile != null){
                        Puddles.deposit(tile, liquid, amount * scaling);
                    }
                }
            }
        }
    }
}
