package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;

public class Compressor extends PowerCrafter {

    public Compressor(String name) {
        super(name);
        hasLiquids = true;
    }

    @Override
    public void draw(Tile tile) {
        Draw.rect(name, tile.drawx(), tile.drawy());
        Draw.color(Color.CLEAR, tile.entity.liquid.liquid.color, tile.entity.liquid.amount / liquidCapacity);
        Draw.rect(name + "-liquid", tile.drawx(), tile.drawy());
        Draw.color();
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-top")};
    }
}
