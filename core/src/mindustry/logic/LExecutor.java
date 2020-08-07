package mindustry.logic;

import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;

public class LExecutor{
    LInstruction[] instructions;
    int counter;

    //values of all the variables
    Var[] vars;

    public boolean initialized(){
        return instructions != null && vars != null && instructions.length > 0;
    }

    /** Runs all the instructions at once. Debugging only. */
    public void runAll(){
        counter = 0;

        while(counter < instructions.length){
            instructions[counter++].run(this);
        }
    }

    public void load(Object context, LAssembler builder){
        builder.putConst("this", context);

        vars = new Var[builder.vars.size];
        instructions = builder.instructions;
        counter = 0;

        builder.vars.each((name, var) -> {
            Var v = new Var();
            vars[var.id] = v;

            if(var.constant){
                v.constant = true;
                if(var.value instanceof Number){
                    v.numval = ((Number)var.value).doubleValue();
                }else{
                    v.isobj = true;
                    v.objval = var.value;
                }
            }
        });
    }

    //region utility

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

    String str(int index){
        Var v = vars[index];
        return v.isobj ? String.valueOf(v.objval) : String.valueOf(v.numval);
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

    //endregion

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

        ToggleI(){}

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

        AssignI(){}

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
        public BinaryOp op;
        public int a, b, dest;

        public BinaryOpI(BinaryOp op, int a, int b, int dest){
            this.op = op;
            this.a = a;
            this.b = b;
            this.dest = dest;
        }

        BinaryOpI(){}

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

    public static class PrintI implements LInstruction{
        public int value;

        public PrintI(int value){
            this.value = value;
        }

        PrintI(){}

        @Override
        public void run(LExecutor exec){
            Log.info(exec.str(value));
        }
    }

    public static class JumpI implements LInstruction{
        public int cond, to;

        public JumpI(int cond, int to){
            this.cond = cond;
            this.to = to;
        }

        JumpI(){}

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

        FetchBuildI(){}

        @Override
        public void run(LExecutor exec){
            exec.setobj(dest, Vars.world.build(exec.numi(x), exec.numi(y)));
        }
    }

    //endregion
}
