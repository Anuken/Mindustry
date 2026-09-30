package mindustry.logic.instructions;

import mindustry.gen.*;
import mindustry.logic.*;

public class ClientDataI implements LogicInstruction{
    public LogicVar channel, value, reliable;

    public ClientDataI(LogicVar channel, LogicVar value, LogicVar reliable){
        this.channel = channel;
        this.value = value;
        this.reliable = reliable;
    }

    public ClientDataI(){
    }

    @Override
    public void run(LogicExecutor exec){
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
