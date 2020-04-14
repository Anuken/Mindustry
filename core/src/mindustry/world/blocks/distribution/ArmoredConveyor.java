package mindustry.world.blocks.distribution;

import mindustry.type.*;
import mindustry.world.*;

public class ArmoredConveyor extends Conveyor{

    public ArmoredConveyor(String name){
        super(name);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return super.acceptItem(item, tile, source) && (source.block() instanceof Conveyor || Edges.getFacingEdge(source, tile).relativeTo(tile) == tile.rotation());
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock);
    }
}
