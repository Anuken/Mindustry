package mindustry.world.blocks.distribution;

import mindustry.type.*;
import mindustry.world.*;

public class ArmoredConveyor extends Conveyor{

    public ArmoredConveyor(String name){
        super(name);
    }

    @Override
    public boolean acceptItem(Tile tile, Tile source, Item item){
        return super.acceptItem(tile, source, item) && (source.block() instanceof Conveyor || Edges.getFacingEdge(source, tile).relativeTo(tile) == tile.rotation());
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock);
    }
}
