package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("select")
public class SelectStatement extends LStatement{
    public String result = "result";
    public ConditionOp op = ConditionOp.notEqual;
    public String comp0 = "x", comp1 = "false", a = "a", b = "b";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    private void rebuild(Table table){
        table.clearChildren();
        table.left();

        Table t = table;

        field(t, result, str -> result = str);
        t.add(" = ");
        t.add(bundle("if"));

        JumpStatement.addOp(this, t, op, o -> {
            op = o;
            rebuild(table);
        }, comp0, str -> comp0 = str, comp1, str -> comp1 = str);

        t.add(bundle("then"));
        field(t, a, str -> a = str).width(130f);
        t.add(bundle("else"));
        field(t, b, str -> b = str).width(130f);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new SelectI(op, builder.var(result), builder.var(comp0), builder.var(comp1), builder.var(a), builder.var(b));
    }

    @Override
    public LCategory category(){
        return LCategory.operation;
    }
}
