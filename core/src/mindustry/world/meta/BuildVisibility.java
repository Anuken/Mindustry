package mindustry.world.meta;

import arc.func.*;
import mindustry.*;

public class BuildVisibility{
    public static final BuildVisibility

    hidden = new BuildVisibility(() -> false),
    shown = new BuildVisibility(() -> true),
    debugOnly = new BuildVisibility(() -> false),
    editorOnly = new BuildVisibility(() -> Vars.state.rules.editor),
    sandboxOnly = new BuildVisibility(() -> Vars.state == null || Vars.state.rules.infiniteResources),
    campaignOnly = new BuildVisibility(() -> Vars.state == null || Vars.state.isCampaign()),
    lightingOnly = new BuildVisibility(() -> Vars.state == null || Vars.state.rules.lighting || Vars.state.isCampaign()),
    ammoOnly = new BuildVisibility(() -> Vars.state == null || Vars.state.rules.unitAmmo),
    fogOnly = new BuildVisibility(() -> Vars.state == null || Vars.state.rules.fog || Vars.state.rules.editor);

    private final Boolp visible;

    public boolean visible(){
        return visible.get();
    }

    public BuildVisibility(Boolp visible){
        this.visible = visible;
    }
}
