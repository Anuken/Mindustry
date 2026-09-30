package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("packcolor")
public class PackColorStatement extends LStatement{
    public String result = "result", r = "1", g = "0", b = "0", a = "1";

    @Override
    public void build(Table table){
        fields(table, result, str -> result = str);

        table.add(" = ");
        table.add(bundle("pack"));

        fields(table, r, str -> r = str);
        fields(table, g, str -> g = str);
        fields(table, b, str -> b = str);
        fields(table, a, str -> a = str);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new PackColorI(builder.var(result), builder.var(r), builder.var(g), builder.var(b), builder.var(a));
    }

    @Override
    public LCategory category(){
        return LCategory.operation;
    }
}
