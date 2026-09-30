package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("write")
public class WriteStatement extends LStatement{
    public String input = "result", target = "cell1", address = "0";

    @Override
    public void build(Table table){
        table.add(bundle("write")).padLeft(3f);

        field(table, input, str -> input = str);

        table.add(bundle("to"));

        field(table, target, str -> target = str);

        table.add(bundle("at"));

        field(table, address, str -> address = str);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new WriteI(builder.var(target), builder.var(address), builder.var(input));
    }

    @Override
    public LCategory category(){
        return LCategory.io;
    }
}
