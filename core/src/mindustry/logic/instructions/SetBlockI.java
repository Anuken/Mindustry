package mindustry.logic.instructions;

import arc.math.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class SetBlockI implements LogicInstruction{
    public LogicVar x, y;
    public LogicVar block;
    public LogicVar team, rotation;
    public LogicTileLayer layer = LogicTileLayer.block;

    public SetBlockI(LogicVar x, LogicVar y, LogicVar block, LogicVar team, LogicVar rotation, LogicTileLayer layer){
        this.x = x;
        this.y = y;
        this.block = block;
        this.team = team;
        this.rotation = rotation;
        this.layer = layer;
    }

    public SetBlockI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(net.client()) return;

        Tile tile = state.world.tile(x.numi(), y.numi());
        if(tile != null && block.obj() instanceof Block b){
            switch(layer){
                case ore -> {
                    if((b instanceof OverlayFloor || b == Blocks.air) && tile.overlay() != b) tile.setOverlayNet(b);
                }
                case floor -> {
                    if(b instanceof Floor f && tile.floor() != f && !f.isOverlay() && !f.isAir()){
                        tile.setFloorNet(f);
                    }
                }
                case block -> {
                    if(!b.isFloor() || b == Blocks.air){
                        Team t = team.team();
                        if(t == null) t = Team.derelict;

                        if(tile.block() != b || tile.team() != t){
                            tile.setNet(b, t, Mathf.clamp(rotation.numi(), 0, 3));
                        }
                    }
                }
                //building case not allowed
            }
        }
    }
}
