package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("print")
public class PrintStatement extends LogicStatement{
    public String value = "\"frog\"";

    @Override
    public void build(Table table){
        field(table, value, str -> value = str).width(LogicCanvas.getTargetWidth() - Scl.scl(20f)).padRight(3);
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new PrintI(builder.var(value));
    }


    @Override
    public LogicCategory category(){
        return LogicCategory.io;
    }
}
