package mindustry.logic.instructions;

import mindustry.logic.*;

public class SelectI implements LogicInstruction{
    public ConditionOp op = ConditionOp.notEqual;
    public LogicVar result, comp0, comp1, a, b;

    public SelectI(ConditionOp op, LogicVar result, LogicVar comp0, LogicVar comp1, LogicVar a, LogicVar b){
        this.op = op;
        this.result = result;
        this.comp0 = comp0;
        this.comp1 = comp1;
        this.a = a;
        this.b = b;
    }

    public SelectI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(result.constant) return;
        result.set(op.test(comp0, comp1) ? a : b);
    }
}
