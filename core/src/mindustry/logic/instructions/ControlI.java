package mindustry.logic.instructions;

import mindustry.gen.*;
import mindustry.logic.*;

/** Controls a building's state. */
public class ControlI implements LInstruction{
    public LVar target;
    public LAccess type = LAccess.enabled;
    public LVar p1, p2, p3, p4;

    public ControlI(LAccess type, LVar target, LVar p1, LVar p2, LVar p3, LVar p4){
        this.type = type;
        this.target = target;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
    }

    ControlI(){
    }

    @Override
    public void run(LExecutor exec){
        Object obj = target.obj();
        if(obj instanceof Building b && (exec.privileged || (exec.build != null && exec.build.validLink(b)))){

            if(type == LAccess.enabled){
                if(p1.bool()){
                    b.noSleep();
                }else{
                    b.lastDisabler = exec.build;
                }
            }

            if(type.isObj && p1.isobj){
                b.control(exec, type, p1.obj(), p2.num(), p3.num(), p4.num());
            }else{
                b.control(exec, type, p1.num(), p2.num(), p3.num(), p4.num());
            }
        }
    }
}
