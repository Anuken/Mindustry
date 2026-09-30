package mindustry.logic.instructions;

import mindustry.logic.*;

public class DrawFlushI implements LInstruction{
    public LVar target;

    public DrawFlushI(LVar target){
        this.target = target;
    }

    public DrawFlushI(){
    }

    @Override
    public void run(LExecutor exec){
        if(target.building() instanceof LDrawable d && d.drawable(exec)){
            d.draw(exec.graphicsBuffer);
        }
        exec.graphicsBuffer.clear();
    }
}
