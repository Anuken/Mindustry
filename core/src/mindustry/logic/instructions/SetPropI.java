package mindustry.logic.instructions;

import mindustry.logic.*;
import mindustry.type.*;

public class SetPropI implements LogicInstruction{
    public LogicVar type, of, value;

    public SetPropI(LogicVar type, LogicVar of, LogicVar value){
        this.type = type;
        this.of = of;
        this.value = value;
    }

    public SetPropI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(of.obj() instanceof LogicSettable sp){
            Object key = type.obj();
            if(key instanceof LogicProp property){
                if(value.isobj){
                    sp.setProp(property, value.objval);
                }else{
                    sp.setProp(property, value.numval);
                }
            }else if(key instanceof UnlockableContent content){
                sp.setProp(content, value.num());
            }
        }
    }
}
