package mindustry.world.meta;

import arc.func.*;
import mindustry.*;

public enum BuildVisibility{
    hidden(() -> false),
    shown(() -> true),
    debugOnly(() -> false),
    editorOnly(() -> false),
    sandboxOnly(() -> Vars.state == null || Vars.state.rules.infiniteResources),
    campaignOnly(() -> Vars.state == null || Vars.state.isCampaign()),
    lightingOnly(() -> Vars.state == null || Vars.state.rules.lighting || Vars.state.isCampaign()),
    ammoOnly(() -> Vars.state == null || Vars.state.rules.unitAmmo);

    private final Boolp visible;

    public boolean visible(){
        return visible.get();
    }

    BuildVisibility(Boolp visible){
        this.visible = visible;
    }
}
