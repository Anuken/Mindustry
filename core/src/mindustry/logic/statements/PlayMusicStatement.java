package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("playmusic")
public class PlayMusicStatement extends LogicStatement{
    public String name = "\"game1\"", interrupt = "true";

    @Override
    public void build(Table table){
        float width = LogicCanvas.isCompact() ? 100f : 190f;

        fields(table, "music", name, str -> name = str).width(width);

        fields(table, "interrupt", interrupt, str -> interrupt = str).width(width);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new PlayMusicI(builder.var(name), builder.var(interrupt));
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.world;
    }
}
