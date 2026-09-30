package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("getlink")
public class GetLinkStatement extends LStatement{
    public String output = "result", address = "0";

    @Override
    public void build(Table table){
        field(table, output, str -> output = str);

        table.add(" = ");

        table.add(bundle("linknum"));

        field(table, address, str -> address = str);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new GetLinkI(builder.var(output), builder.var(address));
    }

    @Override
    public LCategory category(){
        return LCategory.block;
    }
}
