package mindustry.logic.instructions;

import arc.util.*;
import mindustry.logic.*;

public class OpI implements LogicInstruction{
    public LogicOp op = LogicOp.add;
    public LogicVar a, b, dest;

    public OpI(LogicOp op, LogicVar a, LogicVar b, LogicVar dest){
        this.op = op;
        this.a = a;
        this.b = b;
        this.dest = dest;
    }

    OpI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(op == LogicOp.strictEqual){
            dest.setnum(a.isobj == b.isobj && ((a.isobj && Structs.eq(a.objval, b.objval)) || (!a.isobj && a.numval == b.numval)) ? 1 : 0);
        }else if(op.unary){
            dest.setnum(op.function1.get(a.num()));
        }else{
            if(op.objFunction2 != null && a.isobj && b.isobj){
                //use object function if both are objects
                dest.setnum(op.objFunction2.get(a.obj(), b.obj()));
            }else{
                //otherwise use the numeric function
                dest.setnum(op.function2.get(a.num(), b.num()));
            }

        }
    }
}
