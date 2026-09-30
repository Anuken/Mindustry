package mindustry.logic.instructions;

import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;

public class FetchI implements LInstruction{
    public FetchType type = FetchType.unit;
    public LVar result, team, extra, index;

    public FetchI(FetchType type, LVar result, LVar team, LVar extra, LVar index){
        this.type = type;
        this.result = result;
        this.team = team;
        this.extra = extra;
        this.index = index;
    }

    public FetchI(){
    }

    @Override
    public void run(LExecutor exec){
        int i = index.numi();
        Team t = team.team();
        if(t == null) return;
        TeamData data = t.data();

        switch(type){
            case unit -> {
                UnitType type = extra.obj() instanceof UnitType u ? u : null;
                if(type == null){
                    result.setobj(i < 0 || i >= data.units.size ? null : data.units.get(i));
                }else{
                    var units = data.unitCache(type);
                    result.setobj(units == null || i < 0 || i >= units.size ? null : units.get(i));
                }
            }
            case player -> result.setobj(i < 0 || i >= data.players.size ? null :
            data.players.get(i).unit() instanceof BlockUnitc block ? block.tile() : data.players.get(i).unit());
            case core -> result.setobj(i < 0 || i >= data.cores.size ? null : data.cores.get(i));
            case build -> {
                Block block = extra.obj() instanceof Block b ? b : null;
                if(block == null){
                    result.setobj(i < 0 || i >= data.buildings.size ? null : data.buildings.get(i));
                }else{
                    var builds = data.getBuildings(block);
                    result.setobj(i < 0 || i >= builds.size ? null : builds.get(i));
                }
            }
            case unitCount -> {
                UnitType type = extra.obj() instanceof UnitType u ? u : null;
                if(type == null){
                    result.setnum(data.units.size);
                }else{
                    result.setnum(data.unitCache(type) == null ? 0 : data.unitCache(type).size);
                }
            }
            case coreCount -> result.setnum(data.cores.size);
            case playerCount -> result.setnum(data.players.size);
            case buildCount -> {
                Block block = extra.obj() instanceof Block b ? b : null;
                if(block == null){
                    result.setnum(data.buildings.size);
                }else{
                    result.setnum(data.getBuildings(block).size);
                }
            }
        }
    }
}
