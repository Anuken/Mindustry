package mindustry.logic.statements;

import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("uradar")
public class UnitRadarStatement extends RadarStatement{

    public UnitRadarStatement(){
        radar = "0";
    }

    @Override
    public boolean buildFrom(){
        //do not build the "from" section
        return false;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new RadarI(target1, target2, target3, sort, builder.var("@unit"), builder.var(sortOrder), builder.var(output));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.unit;
    }
}
