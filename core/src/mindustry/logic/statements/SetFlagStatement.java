package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("setflag")
public class SetFlagStatement extends LStatement{
    public String flag = "\"flag\"", value = "true";

    @Override
    public void build(Table table){
        float width = LCanvas.isCompact() ? 100f : 190f;

        fields(table, flag, str -> flag = str).width(width);

        table.add(" = ");

        fields(table, value, str -> value = str).width(width);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new SetFlagI(builder.var(flag), builder.var(value));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
