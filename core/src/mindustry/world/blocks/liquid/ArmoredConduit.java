package mindustry.world.blocks.liquid;

import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class ArmoredConduit extends Conduit{
    public @Load("@-cap") TextureRegion capRegion;

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
        public void draw(){
            super.draw();

            //draw the cap when a conduit would normally leak
            Building next = front();
            if(next != null && next.team == team && next.block.hasLiquids) return;

            Draw.rect(capRegion, x, y, rotdeg());
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return super.acceptLiquid(source, liquid) && (source.block instanceof Conduit ||
                source.tile.absoluteRelativeTo(tile.x, tile.y) == rotation);
        }
    }
}
