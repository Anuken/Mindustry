package mindustry.logic.instructions;

import mindustry.logic.*;

public class DrawFlushI implements LogicInstruction{
    public LogicVar target;

    public DrawFlushI(LogicVar target){
        this.target = target;
    }

    public DrawFlushI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(target.building() instanceof LogicDrawable d && d.drawable(exec)){
            d.draw(exec.graphicsBuffer);
        }
        exec.graphicsBuffer.clear();
    }
}
