package io.anuke.mindustry.world.meta;

import io.anuke.arc.func.*;
import io.anuke.mindustry.*;

public enum BuildVisibility{
    hidden(() -> false),
    shown(() -> true),
    debugOnly(() -> true),
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
