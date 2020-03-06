package mindustry.world.blocks.liquid;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.entities.AllDefs.*;
import mindustry.type.*;
import mindustry.world.*;

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
    public void draw(){
        super.draw();

        // draw the cap when a conduit would normally leak
        Tile next = tile.front();
        if(next != null && next.team() == team && next.block().hasLiquids) return;

        Draw.rect(capRegion, x, y, tile.rotation() * 90);
    }

    @Override
    public boolean acceptLiquid(Tile source, Liquid liquid, float amount){
        return super.acceptLiquid(tile, source, liquid, amount) && (source.block() instanceof Conduit) || Edges.getFacingEdge(source, tile).relativeTo(tile) == tile.rotation();
    }

    @Override
    public boolean blends(int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsLiquid && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock);
    }
}
