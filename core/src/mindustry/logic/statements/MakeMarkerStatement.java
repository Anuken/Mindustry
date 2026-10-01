package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.objectives.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("makemarker")
public class MakeMarkerStatement extends LogicStatement{
    public String type = "shape", id = "0", x = "0", y = "0", replace = "true";

    @Override
    public void build(Table table){
        table.clearChildren();

        table.button(b -> {
            b.label(() -> bundle(type));

            b.clicked(() -> showSelect(b, MapObjectives.allMarkerTypeNames.toArray(String.class), type, t -> {
                type = t;
                build(table);
            }, 2, cell -> cell.size(160, 50)));
        }, Styles.logict, () -> {
        }).size(180, 40).color(table.color).left().padLeft(2);

        fields(table, "id", id, str -> id = str);

        fields(table, "x", x, v2 -> x = v2);

        fields(table, "y", y, v1 -> y = v1);

        String desc = bundle("replace");
        fields(table, desc, replace, v -> replace = v);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new MakeMarkerI(type, builder.var(id), builder.var(x), builder.var(y), builder.var(replace));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
