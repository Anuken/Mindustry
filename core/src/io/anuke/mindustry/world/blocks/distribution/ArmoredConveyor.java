package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

public class ArmoredConveyor extends Conveyor{

    public ArmoredConveyor(String name){
        super(name);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return super.acceptItem(item, tile, source) && (source.block() instanceof Conveyor || Edges.getFacingEdge(source, tile).relativeTo(tile) == tile.rotation());
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsItems() && (Point2.equals(tile.x + Geometry.d4(rotation).x, tile.y + Geometry.d4(rotation).y, otherx, othery)
        || ((!otherblock.rotate && Edges.getFacingEdge(otherblock, otherx, othery, tile) != null &&
            Edges.getFacingEdge(otherblock, otherx, othery, tile).relativeTo(tile) == rotation) || (otherblock.rotate && Point2.equals(otherx + Geometry.d4(otherrot).x, othery + Geometry.d4(otherrot).y, tile.x, tile.y))));
    }
}
