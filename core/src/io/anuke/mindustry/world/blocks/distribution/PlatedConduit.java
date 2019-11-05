package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;

public class PlatedConduit extends Conduit{

    protected TextureRegion capRegion;

    public PlatedConduit(String name) {
        super(name);
        leakRate = 10f;
    }

    @Override
    public void load() {
        super.load();
        capRegion = Core.atlas.find(name + "-cap");
    }

    @Override
    public void draw(Tile tile) {
        super.draw(tile);

        Tile next = tile.getNearby(tile.rotation());
        if (next.getTeam() == tile.getTeam() && next.block().hasLiquids) return;

        Draw.rect(capRegion, tile.drawx(), tile.drawy(), tile.rotation() * 90);
    }
}
