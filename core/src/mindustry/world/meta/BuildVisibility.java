package mindustry.world.meta;

import arc.func.*;
import mindustry.*;
import mindustry.core.*;

public enum BuildVisibility{
    hidden(() -> false),
    shown(() -> true),
    debugOnly(() -> false),
    sandboxOnly(() -> Vars.state.rules.infiniteResources),
    campaignOnly(() -> Vars.world.isZone()),
    lightingOnly(() -> Vars.state.rules.lighting),
    bytelogicOnly(() -> Vars.state.rules.bytelogic || Version.build == -1); // fixme, maybe remove the build check

    private final Boolp visible;

    public boolean visible(){
        return visible.get();
    }

    BuildVisibility(Boolp visible){
        this.visible = visible;
    }
}
