package mindustry.logic.instructions;

import mindustry.logic.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class LookupI implements LogicInstruction{
    public LogicVar dest;
    public LogicVar from;
    public ContentType type;

    public LookupI(LogicVar dest, LogicVar from, ContentType type){
        this.dest = dest;
        this.from = from;
        this.type = type;
    }

    public LookupI(){
    }

    @Override
    public void run(LogicExecutor exec){
        dest.setobj(logicVars.lookupContent(type, from.numi()));
    }
}
