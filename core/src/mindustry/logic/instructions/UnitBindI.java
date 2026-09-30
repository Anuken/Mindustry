package mindustry.logic.instructions;

import arc.struct.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Binds the processor to a unit based on some filters. */
public class UnitBindI implements LogicInstruction{
    public LogicVar type;

    public UnitBindI(LogicVar type){
        this.type = type;
    }

    public UnitBindI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(!exec.privileged && !state.rules.logicUnitControl) return;

        if(exec.binds == null || exec.binds.length != content.units().size){
            exec.binds = new int[content.units().size];
        }

        //binding to `null` was previously possible, but was too powerful and exploitable
        if(type.obj() instanceof UnitType type && type.logicControllable){
            Seq<Unit> seq = exec.team.data().unitCache(type);

            if(seq != null && seq.any()){
                exec.binds[type.id] %= seq.size;
                if(exec.binds[type.id] < seq.size){
                    //bind to the next unit
                    exec.unit.setconst(seq.get(exec.binds[type.id]));
                }
                exec.binds[type.id]++;
            }else{
                //no units of this type found
                exec.unit.setconst(null);
            }
        }else if(type.obj() instanceof Unit u && (u.team == exec.team || exec.privileged) && u.type.logicControllable){
            //bind to specific unit object
            exec.unit.setconst(u);
        }else{
            exec.unit.setconst(null);
        }
    }
}
