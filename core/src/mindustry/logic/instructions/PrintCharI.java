package mindustry.logic.instructions;

import mindustry.logic.*;
import mindustry.type.*;

public class PrintCharI implements LogicInstruction{
    public LogicVar value;

    public PrintCharI(LogicVar value){
        this.value = value;
    }

    PrintCharI(){
    }

    @Override
    public void run(LogicExecutor exec){

        if(exec.textBuffer.length() >= LogicExecutor.maxTextBuffer) return;
        if(value.isobj){
            if(!(value.objval instanceof UnlockableContent cont)) return;
            exec.textBuffer.append((char)cont.emojiChar());
            return;
        }

        exec.textBuffer.append((char)Math.floor(value.numval));
    }
}
