package io.anuke.mindustry.world.blocks.logic;

import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;

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
            facing.entity.control(entity.signal != 0);
        }
    }

    @Override
    public void drawSelect(Tile tile){
        super.drawSelect(tile);

        Tile facing = tile.front();
        if(facing != null && facing.entity != null && facing.block().controllable){
            Drawf.selected(facing.x, facing.y, facing.block(), Pal.accent);
        }
    }
}
