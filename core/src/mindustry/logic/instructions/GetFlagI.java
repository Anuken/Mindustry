package mindustry.logic.instructions;

import mindustry.logic.*;

import static mindustry.Vars.*;

public class GetFlagI implements LogicInstruction{
    public LogicVar result, flag;

    public GetFlagI(LogicVar result, LogicVar flag){
        this.result = result;
        this.flag = flag;
    }

    public GetFlagI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(flag.obj() instanceof String str){
            result.setbool(state.rules.objectiveFlags.contains(str));
        }else{
            result.setobj(null);
        }
    }
}
