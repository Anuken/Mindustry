package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.math.*;
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
    protected boolean blends(Tile tile, int direction){
        Tile other = tile.getNearby(Mathf.mod(tile.rotation() - direction, 4));
        if(other != null) other = other.link();

        return other != null && other.block().outputsItems()
        && ((tile.getNearby(tile.rotation()) == other) || ((!other.block().rotate && Edges.getFacingEdge(other, tile).relativeTo(tile) == tile.rotation()) || (other.block().rotate && other.getNearby(other.rotation()) == tile)));
    }
}
