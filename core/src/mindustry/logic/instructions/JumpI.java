package mindustry.logic.instructions;

import mindustry.logic.*;

public class JumpI implements LInstruction{
    public ConditionOp op = ConditionOp.notEqual;
    public LVar value, compare;
    public int address;

    public JumpI(ConditionOp op, LVar value, LVar compare, int address){
        this.op = op;
        this.value = value;
        this.compare = compare;
        this.address = address;
    }

    public JumpI(){
    }

    @Override
    public void run(LExecutor exec){
        if(address != -1 && op.test(value, compare)){
            exec.counter.numval = address;
        }
    }
}
