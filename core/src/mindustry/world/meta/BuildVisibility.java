package mindustry.world.meta;

import arc.func.*;
import mindustry.*;

public enum BuildVisibility{
    hidden(() -> false),
    shown(() -> true),
    debugOnly(() -> false),
    sandboxOnly(() -> Vars.state.rules.infiniteResources),
    campaignOnly(() -> Vars.world.isZone()),
    lightingOnly(() -> Vars.state.rules.lighting);

    private final Boolp visible;

    public boolean visible(){
        return visible.get();
    }

    BuildVisibility(Boolp visible){
        this.visible = visible;
    }
}
