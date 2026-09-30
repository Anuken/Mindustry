package mindustry.logic.instructions;

import mindustry.logic.*;

import static mindustry.Vars.*;

public class GetFlagI implements LInstruction{
    public LVar result, flag;

    public GetFlagI(LVar result, LVar flag){
        this.result = result;
        this.flag = flag;
    }

    public GetFlagI(){
    }

    @Override
    public void run(LExecutor exec){
        if(flag.obj() instanceof String str){
            result.setbool(state.rules.objectiveFlags.contains(str));
        }else{
            result.setobj(null);
        }
    }
}
