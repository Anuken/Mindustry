package mindustry.logic.instructions;

import arc.struct.*;
import mindustry.logic.*;

public class SenseI implements LogicInstruction{
    public LogicVar from, to, type;

    public SenseI(LogicVar from, LogicVar to, LogicVar type){
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public SenseI(){
    }

    @Override
    public void run(LogicExecutor exec){
        Object target = from.obj();
        Object sense = type.obj();

        if(target == null && sense == LogicProp.dead){
            to.setnum(1);
            return;
        }

        //note that remote units/buildings can be sensed as well
        if(target instanceof LogicSenseable se){
            if(sense instanceof LogicProp la){
                if(exec.privileged || !la.privileged){
                    Object objOut = se.senseObject(la);

                    if(objOut == LogicSenseable.noSensed){
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
                to.setobj(sensed == LogicSenseable.noSensed ? null : sensed);
                return;
            }
        }else{
            if(sense == LogicProp.size || sense == LogicProp.bufferSize){
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
