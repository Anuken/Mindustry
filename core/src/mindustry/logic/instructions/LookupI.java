package mindustry.logic.instructions;

import mindustry.logic.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class LookupI implements LInstruction{
    public LVar dest;
    public LVar from;
    public ContentType type;

    public LookupI(LVar dest, LVar from, ContentType type){
        this.dest = dest;
        this.from = from;
        this.type = type;
    }

    public LookupI(){
    }

    @Override
    public void run(LExecutor exec){
        dest.setobj(logicVars.lookupContent(type, from.numi()));
    }
}
