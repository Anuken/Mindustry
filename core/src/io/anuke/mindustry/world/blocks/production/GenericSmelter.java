package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.renderer;

/** A GenericCrafter with a new glowing region drawn on top. */
public class GenericSmelter extends GenericCrafter{
    public Color flameColor = Color.valueOf("ffc999");
    public TextureRegion topRegion;

    public GenericSmelter(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        GenericCrafterEntity entity = tile.entity();

        //draw glowing center
        if(entity.warmup > 0f && flameColor.a > 0.001f){
            float g = 0.3f;
            float r = 0.06f;
            float cr = Mathf.random(0.1f);

            Draw.alpha(((1f - g) + Mathf.absin(Time.time(), 8f, g) + Mathf.random(r) - r) * entity.warmup);

            Draw.tint(flameColor);
            Fill.circle(tile.drawx(), tile.drawy(), 3f + Mathf.absin(Time.time(), 5f, 2f) + cr);
            Draw.color(1f, 1f, 1f, entity.warmup);
            Draw.rect(topRegion, tile.drawx(), tile.drawy());
            Fill.circle(tile.drawx(), tile.drawy(), 1.9f + Mathf.absin(Time.time(), 5f, 1f) + cr);

            Draw.color();
        }
    }

    @Override
    public void drawLight(Tile tile){
        GenericCrafterEntity entity = tile.entity();

        renderer.lights.add(tile.drawx(), tile.drawy(), (60f + Mathf.absin(10f, 5f)) * entity.warmup * size, flameColor, 0.65f);
    }
}
