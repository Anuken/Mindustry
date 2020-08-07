package mindustry.logic2;

import arc.scene.ui.layout.*;

public abstract class LStatement{

    public abstract void run(LExecutor exec);
    public abstract void build(Table table);
    public abstract LCategory category();

    public String name(){
        return getClass().getSimpleName();
    }
}
