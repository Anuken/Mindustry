package mindustry.ui.dialogs;

import mindustry.*;
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

        cont.add("[accent]Congrations. You done it.[]\n\nThe enemy on " + planet.localizedName + " has been defeated.");

        float playtime = planet.sectors.sumf(s -> s.hasSave() ? s.save.meta.timePlayed : 0) / 1000f;


        show();
    }
}
