package mindustry.io;

import arc.util.*;
import mindustry.world.*;

public class SaveReadState{
    public final WorldContext context;
    public @Nullable String ruleString;

    public SaveReadState(WorldContext context){
        this.context = context;
    }
}
