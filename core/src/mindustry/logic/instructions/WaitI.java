package mindustry.logic.instructions;

import arc.util.*;
import mindustry.logic.*;

public class WaitI implements LogicInstruction{
    public LogicVar value;

    public float curTime;

    public WaitI(LogicVar value){
        this.value = value;
    }

    public WaitI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(value.num() <= 0){
            // Just yield without executing the wait again
            exec.yield = true;
            // Start the next wait afresh ('value' might have been modified remotely by a different processor)
            curTime = 0f;
        }else if(curTime >= value.num()){
            curTime = 0f;
        }else{
            //skip back to self.
            exec.counter.numval--;
            exec.yield = true;
            curTime += Time.delta / 60f;
        }
    }
}
