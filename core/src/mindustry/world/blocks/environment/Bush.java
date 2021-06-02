package mindustry.world.blocks.environment;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.world.*;

public class Bush extends Prop{
    public @Load(value = "@-bot", fallback = "@") TextureRegion botRegion;
    public @Load(value = "@-center") TextureRegion centerRegion;

    public int lobesMin = 7, lobesMax = 7;
    public float botAngle = 60f, origin = 0.1f;
    public float sclMin = 30f, sclMax = 50f, magMin = 5f, magMax = 15f, timeRange = 40f, spread = 0f;

    static Rand rand = new Rand();

    public Bush(String name){
        super(name);
        variants = 0;
    }

    @Override
    public void drawBase(Tile tile){
        rand.setSeed(tile.pos());
        float offset = rand.random(180f);
        int lobes = rand.random(lobesMin, lobesMax);
        for(int i = 0; i < lobes; i++){
            float ba =  i / (float)lobes * 360f + offset + rand.range(spread), angle = ba + Mathf.sin(Time.time + rand.random(0, timeRange), rand.random(sclMin, sclMax), rand.random(magMin, magMax));
            float w = region.width * Draw.scl, h = region.height * Draw.scl;
            var region = Angles.angleDist(ba, 225f) <= botAngle ? botRegion : this.region;

            Draw.rect(region,
                tile.worldx() - Angles.trnsx(angle, origin) + w*0.5f, tile.worldy() - Angles.trnsy(angle, origin),
                w, h,
                origin*4f, h/2f,
                angle
            );
        }

        if(centerRegion.found()){
            Draw.rect(centerRegion, tile.worldx(), tile.worldy());
        }
    }
}
