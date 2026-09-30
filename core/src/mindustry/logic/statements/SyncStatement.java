package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

//TODO: test this first
@RegisterStatement("sync")
public class SyncStatement extends LStatement{
    public String variable = "var";

    @Override
    public void build(Table table){
        fields(table, variable, str -> variable = str).width(190f);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new SyncI(builder.var(variable));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
