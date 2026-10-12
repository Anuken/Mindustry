package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("wait")
public class WaitStatement extends LogicStatement{
    public String value = "0.5";

    @Override
    public void build(Table table){
        field(table, value, str -> value = str);
        table.add(bundle("seconds"));
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new WaitI(builder.var(value));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.control;
    }
}
