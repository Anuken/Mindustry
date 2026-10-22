package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("setflag")
public class SetFlagStatement extends LogicStatement{
    public String flag = "\"flag\"", value = "true";

    @Override
    public void build(Table table){
        float width = LogicCanvas.isCompact() ? 100f : 190f;

        fields(table, flag, str -> flag = str).width(width);

        table.add(" = ");

        fields(table, value, str -> value = str).width(width);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new SetFlagI(builder.var(flag), builder.var(value));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
