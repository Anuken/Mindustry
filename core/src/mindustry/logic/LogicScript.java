package mindustry.logic;

import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;

//TODO: this isn't used in the game yet
public class LogicScript implements JsonSerializable{
    public static final int defaultTimeoutMs = 200, maxTimeoutMs = 2000;

    /** Timeout in milliseconds. 0 = default timeout. */
    int timeout = 0;
    LExecutor executor = new LExecutor();
    boolean resetVars = false;
    String script = "";

    public LogicScript(String script){
        this.script = script;
        LAssembler assembler = LAssembler.assemble(script, true);
        executor.load(assembler);
    }

    LogicScript(){}

    /** Runs the script once. It does not loop. Execution time is limited. */
    public void run(){
        //reset every variable to null at the start of execution.
        if(resetVars){
            for(var v : executor.vars){
                v.isobj = true;
                v.objval = null;
            }
        }

        executor.counter.setnum(0);
        long ms = Time.millis();
        int timeCheck = 0;

        var counter = executor.counter;
        var instructions = executor.instructions;
        int timeout = (this.timeout <= 0 ? defaultTimeoutMs : this.timeout);

        while(counter.numval >= 0 && counter.numval < executor.instructions.length && !executor.stop){
            counter.isobj = false;
            var current = instructions[(int)(counter.numval++)];
            current.run(executor);

            //don't spam millisecond check calls as they can be expensive
            if(++timeCheck >= 100){
                timeCheck = 0;
                if(Time.timeSinceMillis(ms) > timeout){
                    //TODO: warn about a timeout in the console, this should not happen.
                    break;
                }
            }
        }
    }

    @Override
    public void write(Json json){
        json.writeValue("script", script);
        if(timeout > 0) json.writeValue("timeout", timeout);
        if(resetVars) json.writeValue("resetVars", resetVars);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        if(jsonData.isObject()){
            timeout = Math.min(maxTimeoutMs, jsonData.getInt("timeout", 0));
            resetVars = jsonData.getBoolean("resetVars", false);
            script = jsonData.getString("script", "");
        }else{
            script = jsonData.asString();
        }
    }
}
