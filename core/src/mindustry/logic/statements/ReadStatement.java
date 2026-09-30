package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("read")
public class ReadStatement extends LogicStatement{
    public String output = "result", target = "cell1", address = "0";

    @Override
    public void build(Table table){
        table.add(bundle("read")).padLeft(3f);

        field(table, output, str -> output = str);

        table.add(" = ");

        field(table, target, str -> target = str);

        table.add(bundle("at"));

        field(table, address, str -> address = str);
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new ReadI(builder.var(target), builder.var(address), builder.var(output));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.io;
    }
}
