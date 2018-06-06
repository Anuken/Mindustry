package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;

public class Pulverizer extends GenericCrafter {

    public Pulverizer(String name) {
        super(name);
    }

    @Override
    public void draw(Tile tile) {
        GenericCrafterEntity entity = tile.entity();

        Draw.rect(name, tile.drawx(), tile.drawy());
        Draw.rect(name + "-rotator", tile.drawx(), tile.drawy(), entity.totalProgress * 2f);
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-rotator")};
    }
}
