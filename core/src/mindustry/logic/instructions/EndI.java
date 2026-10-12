package mindustry.logic.instructions;

import mindustry.logic.*;

public class EndI implements LogicInstruction{

    @Override
    public void run(LogicExecutor exec){
        exec.counter.numval = exec.instructions.length;
    }
}
