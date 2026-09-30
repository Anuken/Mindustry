package mindustry.logic.instructions;

import arc.*;
import arc.math.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.logic.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class SpawnUnitI implements LogicInstruction{
    public LogicVar type, x, y, rotation, team, result, effect;

    public SpawnUnitI(LogicVar type, LogicVar x, LogicVar y, LogicVar rotation, LogicVar team, LogicVar result, LogicVar effect){
        this.type = type;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.team = team;
        this.result = result;
        this.effect = effect;
    }

    public SpawnUnitI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(net.client()) return;

        Team t = team.team();

        if(t != null && type.obj() instanceof UnitType type && !type.internal && Units.canCreate(t, type)){
            //random offset to prevent stacking
            var unit = type.spawn(t, World.unconv(x.numf()) + Mathf.range(0.01f), World.unconv(y.numf()) + Mathf.range(0.01f), rotation.numf());
            if(effect.bool()){
                state.spawner.spawnEffect(unit);
            }else{
                //manually call events
                unit.unloaded();
                Events.fire(new UnitSpawnEvent(unit));
            }
            result.setobj(unit);
        }
    }
}
