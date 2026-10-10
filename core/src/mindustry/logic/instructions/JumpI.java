package mindustry.logic.instructions;

import mindustry.logic.*;

public class JumpI implements LogicInstruction{
    public ConditionOp op = ConditionOp.notEqual;
    public LogicVar value, compare;
    public int address;

    public JumpI(ConditionOp op, LogicVar value, LogicVar compare, int address){
        this.op = op;
        this.value = value;
        this.compare = compare;
        this.address = address;
    }

    public JumpI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(address != -1 && op.test(value, compare)){
            exec.counter.numval = address;
        }
    }
}
