package mindustry.content;

import mindustry.ctype.*;
import mindustry.type.*;

import static mindustry.content.Planets.*;

public class SectorPresets implements ContentList{
    public static SectorPreset
    groundZero,
    craters, frozenForest, ruinousShores, stainedMountains, tarFields, fungalPass,
    saltFlats, overgrowth,
    desolateRift, nuclearComplex;

    @Override
    public void load(){

        groundZero = new SectorPreset("groundZero", serpulo, 15){{
            alwaysUnlocked = true;
            captureWave = 10;
        }};

        saltFlats = new SectorPreset("saltFlats", serpulo, 101){{

        }};

        frozenForest = new SectorPreset("frozenForest", serpulo, 86){{
            captureWave = 40;
        }};

        craters = new SectorPreset("craters", serpulo, 18){{
            captureWave = 40;
        }};

        ruinousShores = new SectorPreset("ruinousShores", serpulo, 19){{
            captureWave = 40;
        }};

        stainedMountains = new SectorPreset("stainedMountains", serpulo, 20){{
            captureWave = 30;
        }};

        fungalPass = new SectorPreset("fungalPass", serpulo, 21){{

        }};

        overgrowth = new SectorPreset("overgrowth", serpulo, 22){{

        }};

        tarFields = new SectorPreset("tarFields", serpulo, 23){{
            captureWave = 40;
        }};

        desolateRift = new SectorPreset("desolateRift", serpulo, 123){{
            captureWave = 40;
        }};


        nuclearComplex = new SectorPreset("nuclearComplex", serpulo, 130){{
            captureWave = 60;
        }};
    }
}
