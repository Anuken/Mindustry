package mindustry.logic.instructions;

import arc.struct.*;
import mindustry.logic.*;

public class SenseI implements LInstruction{
    public LVar from, to, type;

    public SenseI(LVar from, LVar to, LVar type){
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public SenseI(){
    }

    @Override
    public void run(LExecutor exec){
        Object target = from.obj();
        Object sense = type.obj();

        if(target == null && sense == LAccess.dead){
            to.setnum(1);
            return;
        }

        //note that remote units/buildings can be sensed as well
        if(target instanceof Senseable se){
            if(sense instanceof LAccess la){
                if(exec.privileged || !la.privileged){
                    Object objOut = se.senseObject(la);

                    if(objOut == Senseable.noSensed){
                        //numeric output
                        to.setnum(se.sense(la));
                    }else{
                        //object output
                        to.setobj(objOut);
                    }
                    return;
                }
            }else if(sense != null){ //sense object value
                to.setnum(se.sense(sense));
                return;
            }else if(!type.isobj){ //sense number
                Object sensed = se.senseObject(type.numval);
                //technically there's no need for noSensed sentinel values here, but there's no harm in keeping it
                to.setobj(sensed == Senseable.noSensed ? null : sensed);
                return;
            }
        }else{
            if(sense == LAccess.size || sense == LAccess.bufferSize){
                if(target instanceof CharSequence seq){
                    to.setnum(seq.length());
                    return;
                }else if(target instanceof Seq<?> seq){
                    to.setnum(seq.size);
                    return;
                }
            }
        }

        //unrecognized or unhandled property
        to.setobj(null);
    }
}
