package io.anuke.mindustry.world.blocks.production;

import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class PlasticFormer extends GenericCrafter {

    public PlasticFormer(String name) {
        super(name);
    }

    @Override
    public void draw(Tile tile) {
        super.draw(tile);

        GenericCrafterEntity entity = tile.entity();

        Draw.alpha(Mathf.absin(entity.totalProgress, 3f, 0.9f) * entity.warmup);
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
        Draw.reset();
    }
}
