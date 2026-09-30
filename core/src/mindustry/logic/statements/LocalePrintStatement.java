package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("localeprint")
public class LocalePrintStatement extends LStatement{
    public String value = "\"name\"";

    @Override
    public void build(Table table){
        field(table, value, str -> value = str).width(LCanvas.getTargetWidth() - Scl.scl(20f)).padRight(3);
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new LocalePrintI(builder.var(value));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
