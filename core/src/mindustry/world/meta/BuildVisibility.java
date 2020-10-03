package mindustry.world.meta;

import arc.func.*;
import mindustry.*;

public enum BuildVisibility{
    hidden(() -> false),
    shown(() -> true),
    debugOnly(() -> false),
    sandboxOnly(() -> Vars.state.rules.infiniteResources),
    campaignOnly(() -> Vars.state.isCampaign()),
    lightingOnly(() -> Vars.state.rules.lighting),
    ammoOnly(() -> Vars.state.rules.unitAmmo);

    private final Boolp visible;

    public boolean visible(){
        return visible.get();
    }

    BuildVisibility(Boolp visible){
        this.visible = visible;
    }
}
