package mindustry.logic;

import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;

public class LExecutor{
    //special variables
    public static final int
        varCounter = 0,
        varTime = 1;

    public double[] memory = {};
    public LInstruction[] instructions = {};
    public Var[] vars = {};

    public boolean initialized(){
        return instructions != null && vars != null && instructions.length > 0;
    }

    /** Runs a single instruction. */
    public void runOnce(){
        //set time
        vars[varTime].numval = Time.millis();

        //reset to start
        if(vars[varCounter].numval >= instructions.length) vars[varCounter].numval = 0;

        if(vars[varCounter].numval < instructions.length){
            instructions[(int)(vars[varCounter].numval++)].run(this);
        }
    }

    public void load(String data){
        load(LAssembler.assemble(data));
    }

    /** Loads with a specified assembler. Resets all variables. */
    public void load(LAssembler builder){
        vars = new Var[builder.vars.size];
        instructions = builder.instructions;

        builder.vars.each((name, var) -> {
            Var dest = new Var(name);
            vars[var.id] = dest;

            dest.constant = var.constant;

            if(var.value instanceof Number){
                dest.isobj = false;
                dest.numval = ((Number)var.value).doubleValue();
            }else{
                dest.isobj = true;
                dest.objval = var.value;
            }
        });
    }

    //region utility

    @Nullable Building building(int index){
        Object o = vars[index].objval;
        return vars[index].isobj && o instanceof Building ? (Building)o : null;
    }

    @Nullable Object obj(int index){
        Object o = vars[index].objval;
        return vars[index].isobj ? o : null;
    }

    boolean bool(int index){
        Var v = vars[index];
        return v.isobj ? v.objval != null : Math.abs(v.numval) >= 0.00001;
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

    //endregion

    public static class Var{
        public final String name;

        public boolean isobj, constant;

        public Object objval;
        public double numval;

        public Var(String name){
            this.name = name;
        }
    }

    //region instruction types

    public interface LInstruction{
        void run(LExecutor exec);
    }

    /** Enables/disables a building. */
    public static class EnableI implements LInstruction{
        public int target, value;

        public EnableI(int target, int value){
            this.target = target;
            this.value = value;
        }

        EnableI(){}

        @Override
        public void run(LExecutor exec){
            Building b = exec.building(target);
            if(b != null) b.enabled = exec.bool(value);
        }
    }

    public static class ReadI implements LInstruction{
        public int from, to;

        public ReadI(int from, int to){
            this.from = from;
            this.to = to;
        }

        public ReadI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = exec.numi(from);

            exec.setnum(to,address < 0 || address >= exec.memory.length ? 0 : exec.memory[address]);
        }
    }

    public static class WriteI implements LInstruction{
        public int from, to;

        public WriteI(int from, int to){
            this.from = from;
            this.to = to;
        }

        public WriteI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = exec.numi(to);

            if(address >= 0 && address < exec.memory.length){
                exec.memory[address] = exec.num(from);
            }
        }
    }

    public static class SenseI implements LInstruction{
        public int from, to, type;

        public SenseI(int from, int to, int type){
            this.from = from;
            this.to = to;
            this.type = type;
        }

        public SenseI(){
        }

        @Override
        public void run(LExecutor exec){
            Object target = exec.obj(from);
            Object sense = exec.obj(type);

            double output = 0;

            if(target instanceof Senseable){
                if(sense instanceof Content){
                    output = ((Senseable)target).sense(((Content)sense));
                }else if(sense instanceof LSensor){
                    output = ((Senseable)target).sense(((LSensor)sense));
                }
            }

            exec.setnum(to, output);

        }
    }

    public static class SetI implements LInstruction{
        public int from, to;

        public SetI(int from, int to){
            this.from = from;
            this.to = to;
        }

        SetI(){}

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
            exec.vars[varCounter].numval = exec.instructions.length;
        }
    }

    public static class PrintI implements LInstruction{
        private static final StringBuilder out = new StringBuilder();

        public int value, target;

        public PrintI(int value, int target){
            this.value = value;
            this.target = target;
        }

        PrintI(){}

        @Override
        public void run(LExecutor exec){
            Building b = exec.building(target);

            if(b == null) return;

            //this should avoid any garbage allocation
            Var v = exec.vars[value];
            if(v.isobj && value != 0){
                if(v.objval instanceof String){
                    b.handleString(v.objval);
                }else if(v.objval == null){
                    b.handleString("null");
                }else{
                    b.handleString("[object]");
                }
            }else{
                out.setLength(0);
                //display integer version when possible
                if(Math.abs(v.numval - (int)v.numval) < 0.000001){
                    out.append((int)v.numval);
                }else{
                    out.append(v.numval);
                }
                b.handleString(out);
            }
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
                exec.vars[varCounter].numval = to;
            }
        }
    }

    public static class GetBuildI implements LInstruction{
        public int dest;
        public int x, y;

        public GetBuildI(int dest, int x, int y){
            this.dest = dest;
            this.x = x;
            this.y = y;
        }

        GetBuildI(){}

        @Override
        public void run(LExecutor exec){
            exec.setobj(dest, Vars.world.build(exec.numi(x), exec.numi(y)));
        }
    }

    //endregion
}
