package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("control")
public class ControlStatement extends LogicStatement{
    public LogicProp type = LogicProp.enabled;
    public String target = "block1", p1 = "0", p2 = "0", p3 = "0", p4 = "0";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    void rebuild(Table table){
        table.clearChildren();

        table.left();

        table.add(bundle("set")).padLeft(3f);

        table.button(b -> {
            b.label(() -> bundle(type)).labelAlign(Align.center).grow().wrap();
            b.clicked(() -> showSelect(b, LogicProp.controls, type, t -> {
                type = t;
                rebuild(table);
            }, 2, cell -> cell.size(110, 50)));
        }, Styles.logict, () -> {
        }).size(110, 40).color(table.color).left().padLeft(2);

        table.add(bundle("of")).self(this::param);

        field(table, target, v -> target = v);

        //Q: why don't you just use arrays for this?
        //A: arrays aren't as easy to serialize so the code generator doesn't handle them
        for(int i = 0; i < type.params.length; i++){
            fields(table, type.params[i], type.params.length > 1 && LogicCanvas.isCompact(), i == 0 ? p1 : i == 1 ? p2 : i == 2 ? p3 : p4, i == 0 ? v -> p1 = v : i == 1 ? v -> p2 = v : i == 2 ? v -> p3 = v : v -> p4 = v);
        }
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new ControlI(type, builder.var(target), builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.block;
    }
}
