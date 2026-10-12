package mindustry.logic.instructions;

import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.world.blocks.logic.LogicBlock.*;

public class SyncI implements LogicInstruction{
    //20 syncs per second
    public static long syncInterval = 1000 / 20;

    public LogicVar variable;

    public SyncI(LogicVar variable){
        this.variable = variable;
    }

    public SyncI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(!variable.constant && Time.timeSinceMillis(variable.syncTime) > syncInterval && exec.build != null){
            variable.syncTime = Time.millis();
            Call.syncVariable(exec.build, variable.id, variable.isobj ? variable.objval : variable.numval);
        }
    }

    @Remote(unreliable = true)
    public static void syncVariable(Building building, int variable, Object value){
        if(building instanceof LogicBuild build){
            LogicVar v = build.executor.optionalVar(variable);
            if(v != null && !v.constant){
                if(value instanceof Number n){
                    v.isobj = false;
                    v.numval = n.doubleValue();
                }else{
                    v.isobj = true;
                    v.objval = value;
                }
            }
        }
    }
}
