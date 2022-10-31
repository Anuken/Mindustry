package mindustry.ui.dialogs;

import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.type.*;

public class CampaignCompleteDialog extends BaseDialog{

    public CampaignCompleteDialog(){
        super("");

        addCloseListener();
        shouldPause = true;

        buttons.defaults().size(210f, 64f);
        buttons.button("@menu", Icon.left, () -> {
            hide();
            Vars.ui.paused.runExitSave();
        });

        buttons.button("@continue", Icon.ok, this::hide);
    }

    public void show(Planet planet){
        //TODO obviously needs different text.
        cont.clear();

        cont.add("[accent]Congratulations.\nThe enemy on " + planet.localizedName + " has been defeated.\n\nThe final sector has been conquered.").row();

        float playtime = planet.sectors.sumf(s -> s.hasSave() ? s.save.meta.timePlayed : 0) / 1000f;

        //TODO needs more info
        cont.add("Total Playtime: " + UI.formatTime(playtime)).left().row();

        show();
    }
}
