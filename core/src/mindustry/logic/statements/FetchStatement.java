package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("fetch")
public class FetchStatement extends LogicStatement{
    public FetchType type = FetchType.unit;
    public String result = "result", team = "@sharded", index = "0", extra = "@conveyor";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    void rebuild(Table table){
        table.clearChildren();

        fields(table, result, r -> result = r);

        table.add(" = ");

        table.button(b -> {
            b.label(() -> bundle(type)).growX().wrap().labelAlign(Align.center);
            b.clicked(() -> showSelect(b, FetchType.all, type, o -> {
                type = o;
                rebuild(table);
            }, 2, c -> c.width(150f)));
        }, Styles.logict, () -> {
        }).size(160f, 40f).margin(5f).pad(4f).color(table.color);

        fields(table, "team", team, s -> team = s);

        if(type != FetchType.coreCount && type != FetchType.playerCount && type != FetchType.unitCount && type != FetchType.buildCount){
            fields(table, "#", index, i -> index = i);
        }

        if(type == FetchType.buildCount || type == FetchType.build){
            fields(table, "block", extra, i -> extra = i);
        }

        if(type == FetchType.unitCount || type == FetchType.unit){
            fields(table, "unit", extra, i -> extra = i);
        }
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new FetchI(type, builder.var(result), builder.var(team), builder.var(extra), builder.var(index));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
