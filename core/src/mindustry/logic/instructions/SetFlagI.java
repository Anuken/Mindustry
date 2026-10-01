package mindustry.logic.instructions;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class SetFlagI implements LogicInstruction{
    public LogicVar flag, value;

    public SetFlagI(LogicVar flag, LogicVar value){
        this.flag = flag;
        this.value = value;
    }

    public SetFlagI(){
    }

    @Override
    public void run(LogicExecutor exec){
        //don't invoke unless the flag state actually changes
        if(flag.obj() instanceof String str && state.rules.objectiveFlags.contains(str) != value.bool()){
            Call.setFlag(str, value.bool());
        }
    }

    @Remote(called = Loc.server)
    public static void setFlag(String flag, boolean add){
        if(add){
            state.rules.objectiveFlags.add(flag);
        }else{
            state.rules.objectiveFlags.remove(flag);
        }
    }
}
