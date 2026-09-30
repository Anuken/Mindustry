package mindustry.logic.instructions;

import mindustry.logic.*;

public class StopI implements LInstruction{

    @Override
    public void run(LExecutor exec){
        //skip back to self.
        exec.counter.numval--;
        exec.yield = true;
        exec.stop = true;
    }
}
