package io.anuke.mindustry.world.blocks.effect;

import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;

import static io.anuke.mindustry.Vars.tilesize;

public abstract class EffectCore extends Block{
    protected int range = 7;

    public EffectCore(String name) {
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void drawSelect(Tile tile) {
        Draw.color(Palette.accent);
        Lines.dashCircle(tile.drawx(), tile.drawy(), range * tilesize);
        Draw.reset();
    }
}
