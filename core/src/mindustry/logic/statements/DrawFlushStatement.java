package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("drawflush")
public class DrawFlushStatement extends LogicStatement{
    public String target = "display1";

    @Override
    public void build(Table table){
        table.add(bundle("to")).padLeft(3f);
        field(table, target, str -> target = str);
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new DrawFlushI(builder.var(target));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.block;
    }
}
