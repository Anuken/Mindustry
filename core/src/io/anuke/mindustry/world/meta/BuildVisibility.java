package io.anuke.mindustry.world.meta;

import io.anuke.arc.function.*;
import io.anuke.mindustry.*;

public enum BuildVisibility{
    hidden(() -> false),
    shown(() -> true),
    debugOnly(() -> false),
    sandboxOnly(() -> Vars.state.rules.infiniteResources),
    campaignOnly(() -> Vars.world.isZone());

    private final BooleanProvider visible;

    public boolean visible(){
        return visible.get();
    }

    BuildVisibility(BooleanProvider visible){
        this.visible = visible;
    }
}
