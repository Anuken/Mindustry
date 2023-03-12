package mindustry.world.blocks.distribution;

import arc.math.geom.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class ArmoredConveyor extends Conveyor{

    public ArmoredConveyor(String name){
        super(name);
        noSideBlend = true;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return (otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock)) ||
            (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasItems);
    }

    @Override
    public boolean blendsArmored(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return Point2.equals(tile.x + Geometry.d4(rotation).x, tile.y + Geometry.d4(rotation).y, otherx, othery)
            || ((!otherblock.rotatedOutput(otherx, othery) && Edges.getFacingEdge(otherblock, otherx, othery, tile) != null &&
            Edges.getFacingEdge(otherblock, otherx, othery, tile).relativeTo(tile) == rotation) ||
            (otherblock instanceof Conveyor && otherblock.rotatedOutput(otherx, othery) && Point2.equals(otherx + Geometry.d4(otherrot).x, othery + Geometry.d4(otherrot).y, tile.x, tile.y)));
    }

    public class ArmoredConveyorBuild extends ConveyorBuild{
        @Override
        public boolean acceptItem(Building source, Item item){
            return super.acceptItem(source, item) && (source.block instanceof Conveyor || Edges.getFacingEdge(source.tile(), tile).relativeTo(tile) == rotation);
        }
    }
}
