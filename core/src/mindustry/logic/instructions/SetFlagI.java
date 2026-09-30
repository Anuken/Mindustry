package mindustry.logic.instructions;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class SetFlagI implements LInstruction{
    public LVar flag, value;

    public SetFlagI(LVar flag, LVar value){
        this.flag = flag;
        this.value = value;
    }

    public SetFlagI(){
    }

    @Override
    public void run(LExecutor exec){
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
