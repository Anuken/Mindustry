package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

import static mindustry.logic.LCanvas.*;

@RegisterStatement("setblock")
public class SetBlockStatement extends LStatement{
    public TileLayer layer = TileLayer.block;
    public String block = "@air", x = "0", y = "0", team = "@derelict", rotation = "0";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    void rebuild(Table table){
        table.clearChildren();

        table.add(bundle("set")).padRight(4f).padLeft(6f);

        table.button(b -> {
            b.label(() -> bundle(layer));
            b.clicked(() -> showSelect(b, TileLayer.settable, layer, o -> {
                layer = o;
                rebuild(table);
            }));
        }, Styles.logict, () -> {
        }).size(100f, 40f).pad(4f).color(table.color);

        if(isCompact()) table.add().width(200f);

        fields(table, "x", x, str -> x = str);
        fields(table, "y", y, str -> y = str);
        fields(table, "block", block, str -> block = str);

        if(layer == TileLayer.block){
            fields(table, "team", team, str -> team = str);
            fields(table, "rotation", rotation, str -> rotation = str);
        }
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new SetBlockI(builder.var(x), builder.var(y), builder.var(block), builder.var(team), builder.var(rotation), layer);
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
