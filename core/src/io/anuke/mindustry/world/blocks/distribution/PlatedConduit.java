package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Edges;
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

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return super.acceptLiquid(tile, source, liquid, amount) && (source.block() instanceof Conduit) || Edges.getFacingEdge(source, tile).relativeTo(tile) == tile.rotation();
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsLiquid && (Point2.equals(tile.x + Geometry.d4(rotation).x, tile.y + Geometry.d4(rotation).y, otherx, othery)
                || ((!otherblock.rotate && Edges.getFacingEdge(otherblock, otherx, othery, tile) != null &&
                Edges.getFacingEdge(otherblock, otherx, othery, tile).relativeTo(tile) == rotation) || (otherblock.rotate && Point2.equals(otherx + Geometry.d4(otherrot).x, othery + Geometry.d4(otherrot).y, tile.x, tile.y))));
    }
}
