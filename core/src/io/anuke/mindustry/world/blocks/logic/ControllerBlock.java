package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.tilesize;

public class ControllerBlock extends UnaryLogicBlock{

    public ControllerBlock(String name){
        super(name);
        processor = in -> in;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        LogicEntity entity = tile.entity();
        Tile facing = tile.front();
        if(facing != null && facing.entity != null && facing.block().controllable){
            facing.entity.control(entity.nextSignal != 0);
        }
    }

    @Override
    public void drawSelect(Tile tile){
        super.drawSelect(tile);

        Tile facing = tile.front();
        if(facing != null && facing.entity != null && facing.block().controllable){
            Drawf.selected(facing.x, facing.y, facing.block(), Pal.accent);
        }else{
            Draw.color(Pal.remove);
            Draw.rect(Icon.cancelSmall.getRegion(), tile.drawx() + Geometry.d4(tile.rotation()).x * tilesize, tile.drawy() + Geometry.d4(tile.rotation()).y * tilesize);
            Draw.color();
        }
    }
}
