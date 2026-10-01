package mindustry.logic.instructions;

import mindustry.logic.*;

public class PrintFlushI implements LogicInstruction{
    public LogicVar target;

    public PrintFlushI(LogicVar target){
        this.target = target;
    }

    public PrintFlushI(){
    }

    @Override
    public void run(LogicExecutor exec){

        if(target.building() instanceof LogicPrintable d && d.printable(exec)){
            d.print(exec.textBuffer);
        }
        exec.textBuffer.setLength(0);

    }
}
