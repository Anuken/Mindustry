package mindustry.logic.instructions;

import mindustry.logic.*;

public class SelectI implements LInstruction{
    public ConditionOp op = ConditionOp.notEqual;
    public LVar result, comp0, comp1, a, b;

    public SelectI(ConditionOp op, LVar result, LVar comp0, LVar comp1, LVar a, LVar b){
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
    public void run(LExecutor exec){
        if(result.constant) return;
        result.set(op.test(comp0, comp1) ? a : b);
    }
}
