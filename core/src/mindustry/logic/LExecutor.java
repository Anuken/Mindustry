package mindustry.logic;

import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.gen.*;

public class LExecutor{
    LInstruction[] instructions;
    int counter;

    //values of all the variables
    Var[] vars;

    /** Runs all the instructions at once. Debugging only. */
    public void runAll(){
        counter = 0;

        while(counter < instructions.length){
            instructions[counter++].run(this);
        }
    }

    @Nullable Building building(int index){
        Object o = vars[index].objval;
        return o == null && o instanceof Building ? (Building)o : null;
    }

    boolean bool(int index){
        Var v = vars[index];
        return v.isobj ? v.objval != null : Math.abs(v.numval) < 0.0001;
    }

    double num(int index){
        Var v = vars[index];
        return v.isobj ? 0 : v.numval;
    }

    int numi(int index){
        return (int)num(index);
    }

    void setnum(int index, double value){
        Var v = vars[index];
        if(v.constant) return;
        v.numval = value;
        v.objval = null;
        v.isobj = false;
    }

    void setobj(int index, Object value){
        Var v = vars[index];
        if(v.constant) return;
        v.objval = value;
        v.isobj = true;
    }

    static class Var{
        boolean isobj, constant;

        Object objval;
        double numval;
    }

    //region instruction types

    public interface LInstruction{
        void run(LExecutor exec);
    }

    /** Enables/disables a building. */
    public static class ToggleI implements LInstruction{
        public int target, value;

        public ToggleI(int target, int value){
            this.target = target;
            this.value = value;
        }

        @Override
        public void run(LExecutor exec){
            Building b = exec.building(target);
            if(b != null) b.enabled = exec.bool(value);
        }
    }

    public static class SenseI implements LInstruction{
        public int from, to;

        @Override
        public void run(LExecutor exec){

        }
    }

    public static class AssignI implements LInstruction{
        public int from, to;

        public AssignI(int from, int to){
            this.from = from;
            this.to = to;
        }

        @Override
        public void run(LExecutor exec){
            Var v = exec.vars[to];
            Var f = exec.vars[from];

            //TODO error out when the from-value is a constant
            if(!v.constant){
                if(f.isobj){
                    v.objval = f.objval;
                    v.isobj = true;
                }else{
                    v.numval = f.numval;
                    v.isobj = false;
                }
            }
        }
    }

    public static class BinaryOpI implements LInstruction{
        public mindustry.logic.BinaryOp op;
        public int a, b, dest;

        public BinaryOpI(BinaryOp op, int a, int b, int dest){
            this.op = op;
            this.a = a;
            this.b = b;
            this.dest = dest;
        }

        @Override
        public void run(LExecutor exec){
            exec.setnum(dest, op.function.get(exec.num(a), exec.num(b)));
        }
    }

    public static class EndI implements LInstruction{

        @Override
        public void run(LExecutor exec){
            exec.counter = exec.instructions.length;
        }
    }

    public static class JumpI implements LInstruction{
        public int cond, to;

        public JumpI(int cond, int to){
            this.cond = cond;
            this.to = to;
        }

        @Override
        public void run(LExecutor exec){
            if(to != -1 && exec.bool(cond)){
                exec.counter = to;
            }
        }
    }

    public static class FetchBuildI implements LInstruction{
        public int dest;
        public int x, y;

        public FetchBuildI(int dest, int x, int y){
            this.dest = dest;
            this.x = x;
            this.y = y;
        }

        @Override
        public void run(LExecutor exec){
            exec.setobj(dest, Vars.world.build(exec.numi(x), exec.numi(y)));
        }
    }

    //endregion
}
