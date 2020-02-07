package mindustry.content;

import arc.graphics.Color;
import mindustry.ctype.ContentList;
import mindustry.type.Threshold;

public class Thresholds implements ContentList{
    public static Threshold threshold00, threshold20, threshold40, threshold60, threshold80;

    @Override
    public void load(){
        threshold00 = new Threshold("threshold00"){{
            vaultFrac = 0.001f;
            launcherFrac = 0.001f;
        }};

        threshold20 = new Threshold("threshold20"){{
            vaultFrac = 0.2f;
            launcherFrac = 0.18f;
        }};

        threshold40 = new Threshold("threshold40"){{
            vaultFrac = 0.4f;
            launcherFrac = 0.36f;
        }};

        threshold60 = new Threshold("threshold60"){{
            vaultFrac = 0.6f;
            launcherFrac = 0.54f;
        }};

        threshold80 = new Threshold("threshold80"){{
            vaultFrac = 0.8f;
            launcherFrac = 0.72f;
        }};
    }
}
