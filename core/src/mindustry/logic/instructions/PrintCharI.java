package mindustry.logic.instructions;

import mindustry.logic.*;
import mindustry.type.*;

public class PrintCharI implements LInstruction{
    public LVar value;

    public PrintCharI(LVar value){
        this.value = value;
    }

    PrintCharI(){
    }

    @Override
    public void run(LExecutor exec){

        if(exec.textBuffer.length() >= LExecutor.maxTextBuffer) return;
        if(value.isobj){
            if(!(value.objval instanceof UnlockableContent cont)) return;
            exec.textBuffer.append((char)cont.emojiChar());
            return;
        }

        exec.textBuffer.append((char)Math.floor(value.numval));
    }
}
