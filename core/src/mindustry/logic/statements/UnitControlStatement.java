package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.logic.LCanvas.*;

@RegisterStatement("ucontrol")
public class UnitControlStatement extends LStatement{
    public LUnitControl type = LUnitControl.move;
    public String p1 = "0", p2 = "0", p3 = "0", p4 = "0", p5 = "0";

    @Override
    public void build(Table table){
        table.clearChildren();

        table.button(b -> {
            b.label(() -> bundle(type));
            b.clicked(() -> showSelect(b, Structs.filter(LUnitControl.class, LUnitControl.all, t ->
            t == LUnitControl.build ? state.rules.logicUnitBuild :
            t == LUnitControl.deconstruct ? state.rules.logicUnitDeconstruct :
            true
            ), type, t -> {
                type = t;
                build(table);
            }, 2, cell -> cell.size(120, 50)));
        }, Styles.logict, () -> {
        }).size(180, 40).color(table.color).left().padLeft(2);

        if(isCompact()) table.add().width(200f);

        //Q: why don't you just use arrays for this?
        //A: arrays aren't as easy to serialize so the code generator doesn't handle them
        for(int i = 0; i < type.params.length; i++){
            fields(table, type.params[i], i == 0 ? p1 : i == 1 ? p2 : i == 2 ? p3 : i == 3 ? p4 : p5, i == 0 ? v -> p1 = v : i == 1 ? v -> p2 = v : i == 2 ? v -> p3 = v : i == 3 ? v -> p4 = v : v -> p5 = v);
        }
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new UnitControlI(type, builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4), builder.var(p5));
    }

    @Override
    public LCategory category(){
        return LCategory.unit;
    }
}
