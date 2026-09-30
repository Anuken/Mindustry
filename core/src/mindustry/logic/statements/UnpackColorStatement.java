package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("unpackcolor")
public class UnpackColorStatement extends LStatement{
    public String r = "r", g = "g", b = "b", a = "a", value = "color";

    @Override
    public void build(Table table){
        fields(table, r, str -> r = str);
        fields(table, g, str -> g = str);
        fields(table, b, str -> b = str);
        fields(table, a, str -> a = str);

        table.add(" = ");
        table.add(bundle("unpack"));

        fields(table, value, str -> value = str);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new UnpackColorI(builder.var(r), builder.var(g), builder.var(b), builder.var(a), builder.var(value));
    }

    @Override
    public LCategory category(){
        return LCategory.operation;
    }
}
