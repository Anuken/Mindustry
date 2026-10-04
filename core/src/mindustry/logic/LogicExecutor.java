package mindustry.logic;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.blocks.logic.LogicBlock.*;

public class LogicExecutor{
    public static int maxInstructions = 1000;

    public static final int
    maxGraphicsBuffer = 256,
    maxDisplayBuffer = 1024,
    maxTextBuffer = 400;

    public LogicInstruction[] instructions = {};
    /** Non-constant variables used for network sync */
    public LogicVar[] vars = {};

    public LogicVar counter, unit, thisv, ipt, queryResult;

    public int[] binds;
    public boolean yield, stop;

    public LongSeq graphicsBuffer = new LongSeq();
    public StringBuilder textBuffer = new StringBuilder();
    public Building[] links = {};
    public @Nullable LogicBuild build;
    public IntSet linkIds = new IntSet();
    public Team team = Team.derelict;
    public boolean privileged = false;
    //maps variable name to index in vars; lazily initialized
    protected @Nullable ObjectIntMap<String> nameMap;

    //yes, this is a minor memory leak, but it's probably not significant enough to matter
    protected static IntFloatMap unitTimeouts = new IntFloatMap();

    static{
        Events.on(ResetEvent.class, e -> unitTimeouts.clear());
    }

    public static void runLogicScript(@Nullable String code){
        runLogicScript(code, 100_000, false);
    }

    public static void runLogicScript(@Nullable String code, int maxInstructions, boolean loop){
        if(code == null || code.isEmpty()) return;

        LogicExecutor executor = new LogicExecutor();
        executor.privileged = true;

        try{
            //assembler has no variables, all the standard ones are null
            executor.load(LogicAssembler.assemble(code, true));
        }catch(Throwable ignored){
            return;
        }

        //executions are limited to prevent a game freeze
        for(int i = 1; i < maxInstructions; i++){
            if((!loop && executor.counter.numval >= executor.instructions.length || executor.counter.numval < 0) || executor.yield) break;
            executor.runOnce();
        }
    }

    public boolean timeoutDone(Unit unit, float delay){
        return Vars.state.time >= unitTimeouts.get(unit.id) + delay;
    }

    public void updateTimeout(Unit unit){
        unitTimeouts.put(unit.id, Vars.state.time);
    }

    public boolean initialized(){
        return instructions.length > 0;
    }

    /** Runs a single instruction. */
    public void runOnce(){
        //reset to start
        if(counter.numval >= instructions.length || counter.numval < 0){
            counter.numval = 0;
        }

        if(counter.numval < instructions.length){
            counter.isobj = false;
            instructions[(int)(counter.numval++)].run(this);
        }
    }

    /** Loads with a specified assembler. Resets all variables. */
    public void load(LogicAssembler builder){
        stop = false;
        textBuffer.setLength(0);
        graphicsBuffer.clear();
        nameMap = null;
        //retain constants that are links, which, by convention, don't start with @ (builtin) or _ (numeric constant)
        vars = builder.vars.values().toSeq().retainAll(var -> !var.constant || var.name.charAt(0) != '_' && var.name.charAt(0) != '@').toArray(LogicVar.class);
        for(int i = 0; i < vars.length; i++){
            vars[i].id = i;
        }

        instructions = builder.instructions;
        counter = builder.getVar("@counter");
        unit = builder.getVar("@unit");
        thisv = builder.getVar("@this");
        ipt = builder.putConst("@ipt", build != null ? build.ipt : 0);
        if(builder.privileged) queryResult = builder.putConst("@queries", null);
    }

    public @Nullable LogicVar optionalVar(String name){
        if(nameMap == null){
            nameMap = new ObjectIntMap<>();
            for(int i = 0; i < vars.length; i++){
                nameMap.put(vars[i].name, i);
            }
        }
        return optionalVar(nameMap.get(name, -1));
    }

    /** @return a Var from this processor. May be null if out of bounds. */
    public @Nullable LogicVar optionalVar(int index){
        return index < 0 || index >= vars.length ? null : vars[index];
    }
}
