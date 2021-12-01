package mindustry.world.blocks.environment;

import mindustry.content.*;
import mindustry.world.*;

/** Empty floor is *not* equivalent to air. Unlike air, it is solid, and still draws neighboring tile edges. */
public class EmptyFloor extends Floor{

    public EmptyFloor(String name){
        super(name);
        variants = 0;
        canShadow = false;
        placeableOn = false;
        solid = true;
    }

    @Override
    public void drawBase(Tile tile){
        //draws only edges, never itself
        drawEdges(tile);

        Floor floor = tile.overlay();
        if(floor != Blocks.air && floor != this){
            floor.drawBase(tile);
        }
    }
}
