package mindustry.logic;

import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.blocks.logic.LogicDisplay.*;
import mindustry.world.blocks.logic.MemoryBlock.*;
import mindustry.world.blocks.logic.MessageBlock.*;

import static mindustry.Vars.*;

public class LExecutor{
    public static final int maxInstructions = 1000;

    //for noise operations
    public static final Simplex noise = new Simplex();

    //special variables
    public static final int
        varCounter = 0,
        varTime = 1;

    public static final int
        maxGraphicsBuffer = 256,
        maxDisplayBuffer = 1024,
        maxTextBuffer = 256;

    public LInstruction[] instructions = {};
    public Var[] vars = {};

    public LongSeq graphicsBuffer = new LongSeq();
    public StringBuilder textBuffer = new StringBuilder();
    public Building[] links = {};

    public boolean initialized(){
        return instructions != null && vars != null && instructions.length > 0;
    }

    /** Runs a single instruction. */
    public void runOnce(){
        //set time
        vars[varTime].numval = Time.millis();

        //reset to start
        if(vars[varCounter].numval >= instructions.length
            || vars[varCounter].numval < 0) vars[varCounter].numval = 0;

        if(vars[varCounter].numval < instructions.length){
            instructions[(int)(vars[varCounter].numval++)].run(this);
        }
    }

    public void load(String data, int maxInstructions){
        load(LAssembler.assemble(data, maxInstructions));
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

    public @Nullable Building building(int index){
        Object o = vars[index].objval;
        return vars[index].isobj && o instanceof Building ? (Building)o : null;
    }

    public @Nullable Object obj(int index){
        Object o = vars[index].objval;
        return vars[index].isobj ? o : null;
    }

    public boolean bool(int index){
        Var v = vars[index];
        return v.isobj ? v.objval != null : Math.abs(v.numval) >= 0.00001;
    }

    public double num(int index){
        Var v = vars[index];
        return v.isobj ? v.objval != null ? 1 : 0 : v.numval;
    }

    public int numi(int index){
        return (int)num(index);
    }

    public void setnum(int index, double value){
        Var v = vars[index];
        if(v.constant) return;
        v.numval = value;
        v.objval = null;
        v.isobj = false;
    }

    public void setobj(int index, Object value){
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

    /** Controls a building's state. */
    public static class ControlI implements LInstruction{
        public int target;
        public LAccess type = LAccess.enabled;
        public int p1, p2, p3, p4;

        public ControlI(LAccess type, int target, int p1, int p2, int p3, int p4){
            this.type = type;
            this.target = target;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
        }

        ControlI(){}

        @Override
        public void run(LExecutor exec){
            Object obj = exec.obj(target);
            if(obj instanceof Controllable){
                Controllable cont = (Controllable)obj;
                cont.control(type, exec.num(p1), exec.num(p2), exec.num(p3), exec.num(p4));
            }
        }
    }

    public static class GetLinkI implements LInstruction{
        public int output, index;

        public GetLinkI(int output, int index){
            this.index = index;
            this.output = output;
        }

        public GetLinkI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = exec.numi(index);

            exec.setobj(output, address >= 0 && address < exec.links.length ? exec.links[address] : null);
        }
    }

    public static class ReadI implements LInstruction{
        public int target, position, output;

        public ReadI(int target, int position, int output){
            this.target = target;
            this.position = position;
            this.output = output;
        }

        public ReadI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = exec.numi(position);
            Building from = exec.building(target);

            if(from instanceof MemoryBuild){
                MemoryBuild mem = (MemoryBuild)from;

                exec.setnum(output, address < 0 || address >= mem.memory.length ? 0 : mem.memory[address]);
            }
        }
    }

    public static class WriteI implements LInstruction{
        public int target, position, value;

        public WriteI(int target, int position, int value){
            this.target = target;
            this.position = position;
            this.value = value;
        }

        public WriteI(){
        }

        @Override
        public void run(LExecutor exec){
            int address = exec.numi(position);
            Building from = exec.building(target);

            if(from instanceof MemoryBuild){
                MemoryBuild mem = (MemoryBuild)from;

                if(address >= 0 && address < mem.memory.length){
                    mem.memory[address] = exec.num(value);
                }

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

            if(target instanceof Senseable){
                Senseable se = (Senseable)target;
                if(sense instanceof Content){
                    exec.setnum(to, se.sense(((Content)sense)));
                }else if(sense instanceof LAccess){
                    Object objOut = se.senseObject((LAccess)sense);

                    if(objOut == Senseable.noSensed){
                        //numeric output
                        exec.setnum(to, se.sense((LAccess)sense));
                    }else{
                        //object output
                        exec.setobj(to, objOut);
                    }
                }
            }else{
                exec.setnum(to, 0);
            }
        }
    }

    public static class RadarI implements LInstruction{
        public RadarTarget target1 = RadarTarget.enemy, target2 = RadarTarget.any, target3 = RadarTarget.any;
        public RadarSort sort = RadarSort.distance;
        public int radar, sortOrder, output;

        //radar instructions are special in that they cache their output and only change it at fixed intervals.
        //this prevents lag from spam of radar instructions
        public Healthc lastTarget;
        public Interval timer = new Interval();

        static float bestValue = 0f;
        static Unit best = null;

        public RadarI(RadarTarget target1, RadarTarget target2, RadarTarget target3, RadarSort sort, int radar, int sortOrder, int output){
            this.target1 = target1;
            this.target2 = target2;
            this.target3 = target3;
            this.sort = sort;
            this.radar = radar;
            this.sortOrder = sortOrder;
            this.output = output;
        }

        public RadarI(){
        }

        @Override
        public void run(LExecutor exec){
            Building target = exec.building(radar);

            int sortDir = exec.bool(sortOrder) ? 1 : -1;

            if(target instanceof Ranged){
                float range = ((Ranged)target).range();

                Healthc targeted;

                if(timer.get(30f)){
                    //if any of the targets involve enemies
                    boolean enemies = target1 == RadarTarget.enemy || target2 == RadarTarget.enemy || target3 == RadarTarget.enemy;

                    best = null;
                    bestValue = 0;

                    if(enemies){
                        for(Team enemy : state.teams.enemiesOf(target.team)){
                            find(target, range, sortDir, enemy);
                        }
                    }else{
                        find(target, range, sortDir, target.team);
                    }

                    lastTarget = targeted = best;
                }else{
                    targeted = lastTarget;
                }

                exec.setobj(output, targeted);
            }else{
                exec.setobj(output, null);
            }
        }

        void find(Building b, float range, int sortDir, Team team){
            Units.nearby(team, b.x, b.y, range, u -> {
                if(!u.within(b, range)) return;

                boolean valid =
                    target1.func.get(b.team, u) &&
                    target2.func.get(b.team, u) &&
                    target3.func.get(b.team, u);

                if(!valid) return;

                float val = sort.func.get(b, u) * sortDir;
                if(val > bestValue || best == null){
                    bestValue = val;
                    best = u;
                }
            });
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

    public static class OpI implements LInstruction{
        public LogicOp op = LogicOp.add;
        public int a, b, dest;

        public OpI(LogicOp op, int a, int b, int dest){
            this.op = op;
            this.a = a;
            this.b = b;
            this.dest = dest;
        }

        OpI(){}

        @Override
        public void run(LExecutor exec){
            if(op.unary){
                exec.setnum(dest, op.function1.get(exec.num(a)));
            }else{
                Var va = exec.vars[a];
                Var vb = exec.vars[b];

                if(op.objFunction2 != null && (va.isobj || vb.isobj)){
                    //use object function if provided, and one of the variables is an object
                    exec.setnum(dest, op.objFunction2.get(exec.obj(a), exec.obj(b)));
                }else{
                    //otherwise use the numeric function
                    exec.setnum(dest, op.function2.get(exec.num(a), exec.num(b)));
                }

            }
        }
    }

    public static class EndI implements LInstruction{

        @Override
        public void run(LExecutor exec){
            exec.vars[varCounter].numval = exec.instructions.length;
        }
    }

    public static class NoopI implements LInstruction{
        @Override
        public void run(LExecutor exec){}
    }

    public static class DrawI implements LInstruction{
        public byte type;
        public int target;
        public int x, y, p1, p2, p3, p4;

        public DrawI(byte type, int target, int x, int y, int p1, int p2, int p3, int p4){
            this.type = type;
            this.target = target;
            this.x = x;
            this.y = y;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
        }

        public DrawI(){
        }

        @Override
        public void run(LExecutor exec){
            //graphics on headless servers are useless.
            if(Vars.headless) return;

            //add graphics calls, cap graphics buffer size
            if(exec.graphicsBuffer.size < maxGraphicsBuffer){
                exec.graphicsBuffer.add(DisplayCmd.get(type, exec.numi(x), exec.numi(y), exec.numi(p1), exec.numi(p2), exec.numi(p3), exec.numi(p4)));
            }
        }
    }

    public static class DrawFlushI implements LInstruction{
        public int target;

        public DrawFlushI(int target){
            this.target = target;
        }

        public DrawFlushI(){
        }

        @Override
        public void run(LExecutor exec){
            //graphics on headless servers are useless.
            if(Vars.headless) return;

            Building build = exec.building(target);
            if(build instanceof LogicDisplayBuild){
                LogicDisplayBuild d = (LogicDisplayBuild)build;
                if(d.commands.size + exec.graphicsBuffer.size < maxDisplayBuffer){
                    for(int i = 0; i < exec.graphicsBuffer.size; i++){
                        d.commands.addLast(exec.graphicsBuffer.items[i]);
                    }
                }
                exec.graphicsBuffer.clear();
            }
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

            if(exec.textBuffer.length() >= maxTextBuffer) return;

            //this should avoid any garbage allocation
            Var v = exec.vars[value];
            if(v.isobj && value != 0){
                String strValue =
                    v.objval == null ? "null" :
                    v.objval instanceof String ? (String)v.objval :
                    v.objval instanceof Content ? "[content]" :
                    v.objval instanceof Building ? "[building]" :
                    v.objval instanceof Unit ? "[unit]" :
                    "[object]";

                exec.textBuffer.append(strValue);
            }else{
                //display integer version when possible
                if(Math.abs(v.numval - (long)v.numval) < 0.000001){
                    exec.textBuffer.append((long)v.numval);
                }else{
                    exec.textBuffer.append(v.numval);
                }
            }
        }
    }

    public static class PrintFlushI implements LInstruction{
        public int target;

        public PrintFlushI(int target){
            this.target = target;
        }

        public PrintFlushI(){
        }

        @Override
        public void run(LExecutor exec){

            Building build = exec.building(target);
            if(build instanceof MessageBuild){
                MessageBuild d = (MessageBuild)build;

                d.message.setLength(0);
                d.message.append(exec.textBuffer, 0, Math.min(exec.textBuffer.length(), maxTextBuffer));

                exec.textBuffer.setLength(0);
            }
        }
    }

    public static class JumpI implements LInstruction{
        public ConditionOp op = ConditionOp.notEqual;
        public int value, compare, address;

        public JumpI(ConditionOp op, int value, int compare, int address){
            this.op = op;
            this.value = value;
            this.compare = compare;
            this.address = address;
        }

        public JumpI(){
        }

        @Override
        public void run(LExecutor exec){
            if(address != -1){
                Var va = exec.vars[value];
                Var vb = exec.vars[compare];
                boolean cmp = false;

                if(op.objFunction != null && (va.isobj || vb.isobj)){
                    //use object function if provided, and one of the variables is an object
                    cmp = op.objFunction.get(exec.obj(value), exec.obj(compare));
                }else{
                    cmp = op.function.get(exec.num(value), exec.num(compare));
                }

                if(cmp){
                    exec.vars[varCounter].numval = address;
                }
            }
        }
    }

    //endregion
}
