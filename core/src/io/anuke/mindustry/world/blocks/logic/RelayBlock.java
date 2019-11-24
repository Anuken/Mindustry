package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;

public class RelayBlock extends AcceptorLogicBlock{

    public RelayBlock(String name){
        super(name);
        doOutput = true;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        LogicEntity entity = tile.entity();
        Draw.color(entity.nextSignal > 0 ? Pal.accent : Color.white);
        for(Tile prox : tile.entity.proximity()){
            if(canSignal(tile, prox)){
                Draw.rect(region, tile.drawx(), tile.drawy(), tile.relativeTo(prox) * 90);
            }
        }
        Draw.color();
    }
}
