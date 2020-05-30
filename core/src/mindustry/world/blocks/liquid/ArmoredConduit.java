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
        leakResistance = 10f;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsLiquid && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public Block upgrade(Tile tile){
        return tile.block() instanceof Conduit
            && (tile.left() == null
                    || tile.left().block() instanceof Conduit
                    || !super.blends(tile, tile.rotation(), tile.left().tileX(), tile.left().tileY(), tile.left().rotation(), tile.left().block()))
            && (tile.right() == null
                    || tile.right().block() instanceof Conduit
                    || !super.blends(tile, tile.rotation(), tile.right().tileX(), tile.right().tileY(), tile.right().rotation(), tile.right().block()))
            ? this : null;
    }

    public class ArmoredConduitEntity extends ConduitEntity{
        @Override
        public void draw(){
            super.draw();

            // draw the cap when a conduit would normally leak
            Tilec next = tile.front();
            if(next != null && next.team() == team && next.block().hasLiquids) return;

            Draw.rect(capRegion, x, y, tile.rotdeg());
        }

        @Override
        public boolean acceptLiquid(Tilec source, Liquid liquid, float amount){
            return super.acceptLiquid(source, liquid, amount) && (source.block() instanceof Conduit) || Edges.getFacingEdge(source.tile(), tile).absoluteRelativeTo(tile.x, tile.y) == tile.rotation();
        }
    }
}
