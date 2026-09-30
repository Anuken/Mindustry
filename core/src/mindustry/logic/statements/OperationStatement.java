package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("op")
public class OperationStatement extends LogicStatement{
    public LogicOp op = LogicOp.add;
    public String dest = "result", a = "a", b = "b";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    void rebuild(Table table){
        table.clearChildren();

        field(table, dest, str -> dest = str);

        table.add(" = ");

        if(op.unary){
            opButton(table, table);

            field(table, a, str -> a = str);
        }else{
            //"function"-type operations have the name at the left and arguments on the right
            if(op.func){
                funcs(table, table);
            }else{
                field(table, a, str -> a = str);

                opButton(table, table);

                field(table, b, str -> b = str);
            }
        }
    }

    void funcs(Table table, Table parent){
        opButton(table, parent);

        field(table, a, str -> a = str);

        field(table, b, str -> b = str);
    }

    void opButton(Table table, Table parent){
        table.button(b -> {
            b.label(() -> selectTranslate(op.symbol));
            b.clicked(() -> showSelect(b, LogicOp.all, op, o -> {
                op = o;
                rebuild(parent);
            }, 4, c -> c.width(64f)));
        }, Styles.logict, () -> {
        }).size(64f, 40f).pad(4f).color(table.color);
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new OpI(op, builder.var(a), builder.var(b), builder.var(dest));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.operation;
    }
}
