package mindustry.world.blocks.distribution;

import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class ArmoredConveyor extends Conveyor{

    public ArmoredConveyor(String name){
        super(name);
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public @Nullable Block upgrade(Tile tile){
        return tile != null
            && tile.block() instanceof Conveyor
            && (tile.left() == null
                    || tile.left().block() instanceof Conveyor
                    || !super.blends(tile, tile.rotation(), tile.left().tileX(), tile.left().tileY(), tile.left().rotation(), tile.left().block()))
            && (tile.right() == null
                    || tile.right().block() instanceof Conveyor
                    || !super.blends(tile, tile.rotation(), tile.right().tileX(), tile.right().tileY(), tile.right().rotation(), tile.right().block()))
            ? this : null;
    }

    public class ArmoredConveyorEntity extends ConveyorEntity{
        @Override
        public boolean acceptItem(Tilec source, Item item){
            return super.acceptItem(source, item) && (source.block() instanceof Conveyor || Edges.getFacingEdge(source.tile(), tile).relativeTo(tile) == tile.rotation());
        }
    }
}
