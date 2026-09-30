package mindustry.logic.instructions;

import mindustry.gen.*;
import mindustry.logic.*;

public class ClientDataI implements LInstruction{
    public LVar channel, value, reliable;

    public ClientDataI(LVar channel, LVar value, LVar reliable){
        this.channel = channel;
        this.value = value;
        this.reliable = reliable;
    }

    public ClientDataI(){
    }

    @Override
    public void run(LExecutor exec){
        if(channel.obj() instanceof String c){
            Object v = value.isobj ? value.objval : value.numval;
            if(reliable.bool()){
                Call.clientLogicDataReliable(c, v);
            }else{
                Call.clientLogicDataUnreliable(c, v);
            }
        }
    }
}
