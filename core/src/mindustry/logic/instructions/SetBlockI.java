package mindustry.logic.instructions;

import arc.math.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class SetBlockI implements LInstruction{
    public LVar x, y;
    public LVar block;
    public LVar team, rotation;
    public TileLayer layer = TileLayer.block;

    public SetBlockI(LVar x, LVar y, LVar block, LVar team, LVar rotation, TileLayer layer){
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
    public void run(LExecutor exec){
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
