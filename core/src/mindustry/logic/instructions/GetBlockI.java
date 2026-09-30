package mindustry.logic.instructions;

import arc.math.*;
import mindustry.logic.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class GetBlockI implements LInstruction{
    public LVar x, y;
    public LVar dest;
    public TileLayer layer = TileLayer.block;

    public GetBlockI(LVar x, LVar y, LVar dest, TileLayer layer){
        this.x = x;
        this.y = y;
        this.dest = dest;
        this.layer = layer;
    }

    public GetBlockI(){
    }

    @Override
    public void run(LExecutor exec){
        Tile tile = state.world.tile(Mathf.round(x.numf()), Mathf.round(y.numf()));
        if(tile == null){
            dest.setobj(null);
        }else{
            dest.setobj(switch(layer){
                case floor -> tile.floor();
                case ore -> tile.overlay();
                case block -> tile.block();
                case building -> tile.build;
            });
        }
    }
}
