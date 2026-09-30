package mindustry.logic.instructions;

import arc.math.*;
import mindustry.logic.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class GetBlockI implements LogicInstruction{
    public LogicVar x, y;
    public LogicVar dest;
    public LogicTileLayer layer = LogicTileLayer.block;

    public GetBlockI(LogicVar x, LogicVar y, LogicVar dest, LogicTileLayer layer){
        this.x = x;
        this.y = y;
        this.dest = dest;
        this.layer = layer;
    }

    public GetBlockI(){
    }

    @Override
    public void run(LogicExecutor exec){
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
