package mindustry.world.blocks.logic;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.distribution.Conveyor.*;

import static mindustry.Vars.tilesize;

public class ReverserBlock extends UnaryLogicBlock{

    public ReverserBlock(String name){
        super(name);
        processor = in -> in;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        LogicEntity entity = tile.ent();
        Tile facing = tile.front();
        if(facing != null && facing.entity != null && facing.block().controllable){

            if(    (entity.nextSignal == 0 && (facing.rotation() == 0 || facing.rotation() == 1))
                || (entity.nextSignal != 0 && (facing.rotation() == 2 || facing.rotation() == 3))
            )facing.rotation(Mathf.mod(facing.rotation() + 2, 4));
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
