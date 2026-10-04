package mindustry.logic.instructions;

import mindustry.logic.*;

public class StopI implements LogicInstruction{

    @Override
    public void run(LogicExecutor exec){
        //skip back to self.
        exec.counter.numval--;
        exec.yield = true;
        exec.stop = true;
    }
}
