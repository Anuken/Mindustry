package mindustry.logic.instructions;

import mindustry.logic.*;

public class SetI implements LInstruction{
    public LVar from, to;

    public SetI(LVar from, LVar to){
        this.from = from;
        this.to = to;
    }

    SetI(){
    }

    @Override
    public void run(LExecutor exec){
        if(!to.constant) to.set(from);
    }
}
