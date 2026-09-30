package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.LogicFx.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.logic.LCanvas.*;

@RegisterStatement("effect")
public class EffectStatement extends LStatement{
    public String type = "warn", x = "0", y = "0", sizerot = "2", color = "%ffaaff", data = "";

    @Override
    public void build(Table table){
        table.clearChildren();

        table.button(b -> {
            b.label(() -> bundle(type)).growX().wrap().labelAlign(Align.center);
            b.clicked(() -> ui.effects.show(entry -> {
                type = entry.name;
                build(table);
            }));
        }, Styles.logict, () -> {
        }).size(150f, 40f).margin(5f).pad(4f).color(table.color).colspan(2);

        if(isCompact()) table.add().width(200f);

        EffectEntry entry = LogicFx.get(type);

        fields(table, "x", x, str -> x = str);
        fields(table, "y", y, str -> y = str);
        if(entry != null){
            if(entry.color){
                fields(table, "color", color, str -> color = str).width(120f);
                colpick(table);
            }

            if(entry.size || entry.rotate){
                fields(table, entry.size ? "size" : "rotation", sizerot, str -> sizerot = str);
            }

            if(entry.data != null){
                fields(table, "data", data, str -> data = str);
            }
        }
    }

    void colpick(Table table){
        col(table, color, res -> {
            color = "%" + res.toString().substring(0, res.a >= 1f ? 6 : 8);
            build(table);
        });
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler b){
        return new EffectI(LogicFx.get(type), b.var(x), b.var(y), b.var(sizerot), b.var(color), b.var(data));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
