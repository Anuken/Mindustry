package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("bullet")
public class SpawnBulletStatement extends LogicStatement{
    public String result = "result", from = "@dagger", index = "0", x = "x", y = "y", rotation = "angle", team = "null", owner = "null", damage = "-1", velocityScl = "1", lifeScl = "1", aimX = "-1", aimY = "-1";

    @Override
    public void build(Table table){
        fields(table, "-bullet", true, result, str -> result = str);

        fields(table, "from", from, str -> from = str);
        fields(table, "index", index, str -> index = str);
        fields(table, "x", x, str -> x = str);
        fields(table, "y", y, str -> y = str);
        fields(table, "rotation", rotation, str -> rotation = str);
        fields(table, "team", team, str -> team = str);
        fields(table, "owner", owner, str -> owner = str);
        fields(table, "damage", damage, str -> damage = str);
        fields(table, "velocityScl", velocityScl, str -> velocityScl = str);
        fields(table, "lifeScl", lifeScl, str -> lifeScl = str);
        fields(table, "aimX", aimX, str -> aimX = str);
        fields(table, "aimY", aimY, str -> aimY = str);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new SpawnBulletI(
        builder.var(result), builder.var(from), builder.var(index), builder.var(x), builder.var(y), builder.var(rotation),
        builder.var(team), builder.var(owner), builder.var(damage), builder.var(velocityScl), builder.var(lifeScl),
        builder.var(aimX), builder.var(aimY)
        );
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
