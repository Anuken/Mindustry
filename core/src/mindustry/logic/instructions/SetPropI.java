package mindustry.logic.instructions;

import mindustry.logic.*;
import mindustry.type.*;

public class SetPropI implements LInstruction{
    public LVar type, of, value;

    public SetPropI(LVar type, LVar of, LVar value){
        this.type = type;
        this.of = of;
        this.value = value;
    }

    public SetPropI(){
    }

    @Override
    public void run(LExecutor exec){
        if(of.obj() instanceof Settable sp){
            Object key = type.obj();
            if(key instanceof LAccess property){
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
