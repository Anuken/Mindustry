package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

import static mindustry.Vars.*;

@RegisterStatement("clientdata")
public class ClientDataStatement extends LStatement{
    public String channel = "\"frog\"", value = "\"bar\"", reliable = "0";

    @Override
    public void build(Table table){
        table.add("send ");
        fields(table, value, str -> value = str);
        table.add(" on ");
        fields(table, channel, str -> channel = str);
        table.add(", reliable ");
        fields(table, reliable, str -> reliable = str);
    }

    @Override
    public boolean hidden(){
        return true;
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        if(!state.rules.allowLogicData) return null;
        return new ClientDataI(builder.var(channel), builder.var(value), builder.var(reliable));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
