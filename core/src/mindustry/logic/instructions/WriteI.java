package mindustry.logic.instructions;

import mindustry.logic.*;

public class WriteI implements LInstruction{
    public LVar target, position, value;

    public WriteI(LVar target, LVar position, LVar value){
        this.target = target;
        this.position = position;
        this.value = value;
    }

    public WriteI(){
    }

    @Override
    public void run(LExecutor exec){
        Object targetObj = target.obj();
        if(targetObj instanceof LWritable write){
            if(!write.writable(exec)) return;
            write.write(position, value);
        }
    }
}
