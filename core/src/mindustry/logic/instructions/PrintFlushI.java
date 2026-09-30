package mindustry.logic.instructions;

import mindustry.logic.*;

public class PrintFlushI implements LInstruction{
    public LVar target;

    public PrintFlushI(LVar target){
        this.target = target;
    }

    public PrintFlushI(){
    }

    @Override
    public void run(LExecutor exec){

        if(target.building() instanceof LPrintable d && d.printable(exec)){
            d.print(exec.textBuffer);
        }
        exec.textBuffer.setLength(0);

    }
}
