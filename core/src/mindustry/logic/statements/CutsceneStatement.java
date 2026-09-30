package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

import static mindustry.logic.LCanvas.*;

@RegisterStatement("cutscene")
public class CutsceneStatement extends LStatement{
    public CutsceneAction action = CutsceneAction.pan;
    public String p1 = "100", p2 = "100", p3 = "0.06", p4 = "0";

    @Override
    public void build(Table table){
        rebuild(table);
    }

    void rebuild(Table table){
        table.clearChildren();

        table.button(b -> {
            b.label(() -> bundle(action)).growX().wrap().labelAlign(Align.center);
            b.clicked(() -> showSelect(b, CutsceneAction.all, action, o -> {
                action = o;
                rebuild(table);
            }, 3, cell -> cell.size(120f, 40f)));
        }, Styles.logict, () -> {
        }).size(120f, 40f).padLeft(2).color(table.color);

        if(isCompact()) table.add().width(200f);

        switch(action){
            case active, getHud -> {
                fields(table, "result", p1, str -> p1 = str);
            }
            case pan -> {
                fields(table, "x", p1, str -> p1 = str);
                fields(table, "y", p2, str -> p2 = str);
                fields(table, "speed", p3, str -> p3 = str);
            }
            case zoom -> {
                fields(table, "level", p1, str -> p1 = str);
            }
            case shake -> {
                fields(table, "amount", p1, str -> p1 = str);
                fields(table, "duration", p2, str -> p2 = str);
            }
            case setHud -> {
                fields(table, "shown", p1, str -> p1 = str);
            }
        }
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new CutsceneI(action, builder.var(p1), builder.var(p2), builder.var(p3), builder.var(p4));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
