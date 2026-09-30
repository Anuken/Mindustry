package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("setrule")
public class SetRuleStatement extends LStatement{
    public LogicRule rule = LogicRule.waveSpacing;
    public String value = "10", p1 = "0", p2 = "0", p3 = "100", p4 = "100";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    void rebuild(Table table){
        table.clearChildren();

        table.button(b -> {
            b.label(() -> bundle(rule)).growX().wrap().labelAlign(Align.center);
            b.clicked(() -> showSelect(b, LogicRule.all, rule, o -> {
                rule = o;
                rebuild(table);
            }, 2, c -> c.width(150f)));
        }, Styles.logict, () -> {
        }).size(160f, 40f).margin(5f).pad(4f).color(table.color);

        switch(rule){
            case mapArea -> {
                table.add(" = ");

                fields(table, "x", p1, s -> p1 = s);
                fields(table, "y", p2, s -> p2 = s);
                fields(table, "w", p3, s -> p3 = s);
                fields(table, "h", p4, s -> p4 = s);
            }
            case buildSpeed, unitHealth, unitBuildSpeed, unitMineSpeed, unitCost, unitDamage, blockHealth, blockDamage, rtsMinSquad, rtsMinWeight -> {
                if(p1.equals("0")){
                    p1 = "@sharded";
                }

                fields(table, "of", p1, s -> p1 = s);
                table.add(" = ");
                field(table, value, s -> value = s);
            }
            case ban, unban -> {
                table.add(bundle("block-unit"));

                fields(table, value, s -> value = s);
            }
            default -> {
                table.add(" = ");

                field(table, value, s -> value = s);
            }
        }
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new SetRuleI(rule, builder.var(value), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
