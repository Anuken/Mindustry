package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class PlastaniumCompressor extends GenericCrafter{
    protected TextureRegion topRegion;

    public PlastaniumCompressor(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();

        topRegion = Draw.region(name + "-top");
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        GenericCrafterEntity entity = tile.entity();

        Draw.alpha(Mathf.absin(entity.totalProgress, 3f, 0.9f) * entity.warmup);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Draw.reset();
    }
}
