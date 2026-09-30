package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("setrate")
public class SetRateStatement extends LogicStatement{
    public String amount = "10";

    @Override
    public void build(Table table){
        fields(table, "ipt = ", amount, str -> amount = str);
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new SetRateI(builder.var(amount));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.control;
    }
}
