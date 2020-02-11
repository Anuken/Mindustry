package mindustry.world.blocks.logic;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class RelayBlock extends AcceptorLogicBlock{

    public RelayBlock(String name){
        super(name);
        doOutput = true;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        LogicEntity entity = tile.ent();
        Draw.color(entity.nextSignal > 0 ? Pal.accent : Color.white);
        for(Tile prox : tile.entity.proximity()){
            if(canSignal(tile, prox)){
                Draw.rect(region, tile.drawx(), tile.drawy(), tile.relativeTo(prox) * 90);
            }
        }
        Draw.color();
    }
}
