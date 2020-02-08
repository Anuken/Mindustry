package mindustry.content;

import arc.graphics.Color;
import mindustry.ctype.ContentList;
import mindustry.type.Threshold;

public class Thresholds implements ContentList{
    public static Threshold threshold00, threshold20, threshold40, threshold60, threshold80;

    @Override
    public void load(){
        threshold00 = new Threshold("threshold00"){{
            description= "0%";
            vaultFrac = 0.0f;
            launcherFrac = 0.0f;
        }};

        threshold20 = new Threshold("threshold25"){{
            description= "25%";
            vaultFrac = 0.25f;
            launcherFrac = 0.25f;
        }};

        threshold40 = new Threshold("threshold50"){{
            description= "50%";
            vaultFrac = 0.5f;
            launcherFrac = 0.50f;
        }};

        threshold60 = new Threshold("threshold75"){{
            description= "75%";
            vaultFrac = 0.75f;
            launcherFrac = 0.70f; // cap launchers threshold a little lower since they have a total item limit instead of per-item limit
        }};
    }
}
