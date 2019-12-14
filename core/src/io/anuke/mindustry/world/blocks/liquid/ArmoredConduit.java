package io.anuke.mindustry.world.blocks.liquid;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;

public class ArmoredConduit extends Conduit{
    public TextureRegion capRegion;

    public ArmoredConduit(String name){
        super(name);
        leakResistance = 10f;
    }

    @Override
    public void load(){
        super.load();
        capRegion = Core.atlas.find(name + "-cap");
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        // draw the cap when a conduit would normally leak
        Tile next = tile.front();
        if(next != null && next.getTeam() == tile.getTeam() && next.block().hasLiquids) return;

        Draw.rect(capRegion, tile.drawx(), tile.drawy(), tile.rotation() * 90);
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return super.acceptLiquid(tile, source, liquid, amount) && (source.block() instanceof Conduit) || Edges.getFacingEdge(source, tile).relativeTo(tile) == tile.rotation();
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsLiquid && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock);
    }
}
