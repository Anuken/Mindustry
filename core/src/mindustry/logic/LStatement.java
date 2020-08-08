package mindustry.logic;

import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.logic.LCanvas.*;
import mindustry.logic.LExecutor.*;

/**
 * A statement is an intermediate representation of an instruction, to be used mostly in UI.
 * Contains all relevant variable information. */
public abstract class LStatement{
    public transient @Nullable StatementElem elem;

    public abstract void build(Table table);
    public abstract LCategory category();
    public abstract LInstruction build(LAssembler builder);

    public void write(StringBuilder builder){
        LogicIO.write(this,builder);
    }

    public void setupUI(){

    }

    public void saveUI(){

    }

    public String name(){
        return getClass().getSimpleName().replace("Statement", "");
    }
}
