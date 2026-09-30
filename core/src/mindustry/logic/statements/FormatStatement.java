package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("format")
public class FormatStatement extends LStatement{
    public String value = "\"frog\"";

    @Override
    public void build(Table table){
        field(table, value, str -> value = str).width(LCanvas.getTargetWidth() - Scl.scl(20f)).padRight(3);
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new FormatI(builder.var(value));
    }


    @Override
    public LCategory category(){
        return LCategory.io;
    }
}
