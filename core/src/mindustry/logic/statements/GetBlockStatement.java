package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("getblock")
public class GetBlockStatement extends LogicStatement{
    public LogicTileLayer layer = LogicTileLayer.block;
    public String result = "result", x = "0", y = "0";

    @Override
    public void build(Table table){
        fields(table, " =", true, result, str -> result = str);

        table.table(t -> {
            t.button(b -> {
                b.label(() -> bundle(layer));
                b.clicked(() -> showSelect(b, LogicTileLayer.all, layer, o -> layer = o));
            }, Styles.logict, () -> {
            }).size(120f, 40f).pad(4f).color(table.color);
        });

        fields(table, "x", x, str -> x = str);
        fields(table, "y", y, str -> y = str);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new GetBlockI(builder.var(x), builder.var(y), builder.var(result), layer);
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
