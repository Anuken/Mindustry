package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

import static mindustry.logic.LogicCanvas.*;

@RegisterStatement("message")
public class FlushMessageStatement extends LogicStatement{
    public MessageType type = MessageType.announce;
    public String duration = "3", outSuccess = "@wait";

    @Override
    public void build(Table table){
        table.clearChildren();

        table.button(b -> {
            b.label(() -> bundle(type)).growX().wrap().labelAlign(Align.center);
            b.clicked(() -> showSelect(b, MessageType.all, type, o -> {
                type = o;
                build(table);
            }, 2, c -> c.width(150f)));
        }, Styles.logict, () -> {
        }).size(160f, 40f).padLeft(2).color(table.color);

        if(isCompact()) table.add().width(200f);

        switch(type){
            case announce, toast -> {
                fields(table, "duration", duration, str -> duration = str);
            }
        }
        fields(table, "success", outSuccess, str -> outSuccess = str);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new FlushMessageI(type, builder.var(duration), builder.var(outSuccess));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
