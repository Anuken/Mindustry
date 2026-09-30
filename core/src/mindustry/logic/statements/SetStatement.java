package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("set")
public class SetStatement extends LStatement{
    public String to = "result";
    public String from = "0";

    @Override
    public void build(Table table){
        field(table, to, str -> to = str);

        table.add(" = ");

        field(table, from, str -> from = str);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new SetI(builder.var(from), builder.var(to));
    }

    @Override
    public LCategory category(){
        return LCategory.operation;
    }
}
