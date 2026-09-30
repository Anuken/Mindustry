package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("getflag")
public class GetFlagStatement extends LStatement{
    public String result = "result", flag = "\"flag\"";

    @Override
    public void build(Table table){
        float width = LCanvas.isCompact() ? 100f : 190f;

        fields(table, result, str -> result = str).width(width);

        table.add(" = ");
        table.add(bundle("flag"));

        fields(table, flag, str -> flag = str).width(width);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new GetFlagI(builder.var(result), builder.var(flag));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
