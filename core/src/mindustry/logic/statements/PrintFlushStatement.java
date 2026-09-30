package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("printflush")
public class PrintFlushStatement extends LogicStatement{
    public String target = "message1";

    @Override
    public void build(Table table){
        table.add(bundle("to"));
        field(table, target, str -> target = str);
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new PrintFlushI(builder.var(target));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.block;
    }
}
