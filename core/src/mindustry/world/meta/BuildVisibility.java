package mindustry.world.meta;

import arc.func.*;
import mindustry.*;
import mindustry.content.*;

public class BuildVisibility{
    public static final Boolp
        hidden = () -> false,
        shown = () -> true,
        debugOnly = () -> false,
        editorOnly = () -> Vars.state.rules.editor,
        sandboxOnly = () -> Vars.state == null || Vars.state.rules.infiniteResources,
        campaignOnly = () -> Vars.state == null || Vars.state.isCampaign(),
        lightingOnly = () -> Vars.state == null || Vars.state.rules.lighting || Vars.state.isCampaign(),
        berylliumOnly = () -> !Vars.state.rules.hiddenBuildItems.contains(Items.beryllium),
        ammoOnly = () -> Vars.state == null || Vars.state.rules.unitAmmo,
        fogOnly = () -> Vars.state == null || Vars.state.rules.fog || Vars.state.rules.editor;
}
