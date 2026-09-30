package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("spawnwave")
public class SpawnWaveStatement extends LStatement{
    public String x = "10", y = "10", natural = "false";

    @Override
    public void build(Table table){
        fields(table, "natural", natural, str -> natural = str);
        fields(table, "x", x, str -> x = str);
        fields(table, "y", y, str -> y = str);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new SpawnWaveI(builder.var(natural), builder.var(x), builder.var(y));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
