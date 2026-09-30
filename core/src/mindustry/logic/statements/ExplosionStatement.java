package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("explosion")
public class ExplosionStatement extends LStatement{
    public String team = "@crux", x = "0", y = "0", radius = "5", damage = "50", air = "true", ground = "true", pierce = "false", effect = "true";

    @Override
    public void build(Table table){
        fields(table, "team", team, str -> team = str);
        fields(table, "x", x, str -> x = str);
        fields(table, "y", y, str -> y = str);
        fields(table, "radius", radius, str -> radius = str);
        fields(table, "damage", damage, str -> damage = str);
        fields(table, "air", air, str -> air = str);
        fields(table, "ground", ground, str -> ground = str);
        fields(table, "pierce", pierce, str -> pierce = str);
        fields(table, "effect", effect, str -> effect = str);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler b){
        return new ExplosionI(b.var(team), b.var(x), b.var(y), b.var(radius), b.var(damage), b.var(air), b.var(ground), b.var(pierce), b.var(effect));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
