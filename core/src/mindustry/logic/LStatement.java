package mindustry.logic;

import arc.scene.ui.layout.*;

/** A statement is an intermediate representation of an instruction, to be used in UI. */
public abstract class LStatement{

    public abstract void build(Table table);
    public abstract LCategory category();
    public abstract LExecutor.LInstruction build(LBuilder builder);

    public String name(){
        return getClass().getSimpleName().replace("Statement", "");
    }
}
