package mindustry.logic.instructions;

import mindustry.logic.*;

public class WriteI implements LogicInstruction{
    public LogicVar target, position, value;

    public WriteI(LogicVar target, LogicVar position, LogicVar value){
        this.target = target;
        this.position = position;
        this.value = value;
    }

    public WriteI(){
    }

    @Override
    public void run(LogicExecutor exec){
        Object targetObj = target.obj();
        if(targetObj instanceof LogicWritable write){
            if(!write.writable(exec)) return;
            write.write(position, value);
        }
    }
}
