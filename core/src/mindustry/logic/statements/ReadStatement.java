package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("read")
public class ReadStatement extends LStatement{
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
    public LInstruction build(LAssembler builder){
        return new ReadI(builder.var(target), builder.var(address), builder.var(output));
    }

    @Override
    public LCategory category(){
        return LCategory.io;
    }
}
