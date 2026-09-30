package mindustry.logic.instructions;

import mindustry.logic.*;

public class SetI implements LogicInstruction{
    public LogicVar from, to;

    public SetI(LogicVar from, LogicVar to){
        this.from = from;
        this.to = to;
    }

    SetI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(!to.constant) to.set(from);
    }
}
