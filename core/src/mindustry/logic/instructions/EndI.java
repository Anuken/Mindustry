package mindustry.logic.instructions;

import mindustry.logic.*;

public class EndI implements LInstruction{

    @Override
    public void run(LExecutor exec){
        exec.counter.numval = exec.instructions.length;
    }
}
