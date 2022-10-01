package mindustry.world.blocks.liquid;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

public class ArmoredConduit extends Conduit{

    public ArmoredConduit(String name){
        super(name);
        leaks = false;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return (otherblock.outputsLiquid && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock)) ||
            (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasLiquids);
    }

    public class ArmoredConduitBuild extends ConduitBuild{
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return super.acceptLiquid(source, liquid) && (tile == null || source.block instanceof Conduit || source.block instanceof DirectionLiquidBridge ||
                source.tile.absoluteRelativeTo(tile.x, tile.y) == rotation);
        }
    }
}
