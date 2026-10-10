package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("spawn")
public class SpawnUnitStatement extends LogicStatement{
    public String type = "@dagger", x = "10", y = "10", rotation = "90", team = "@sharded", result = "result", effect = "true";

    @Override
    public void build(Table table){
        fields(table, "-spawn", true, result, str -> result = str);
        fields(table, "type", type, str -> type = str);
        fields(table, "x", x, str -> x = str);
        fields(table, "y", y, str -> y = str);
        fields(table, "team", team, str -> team = str);
        fields(table, "angle", rotation, str -> rotation = str);
        fields(table, "effect", effect, str -> effect = str);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new SpawnUnitI(builder.var(type), builder.var(x), builder.var(y), builder.var(rotation), builder.var(team), builder.var(result), builder.var(effect));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
