package mindustry.content;

import mindustry.type.*;

import static mindustry.content.Planets.*;

public class SectorPresets{
    public static SectorPreset
    groundZero,
    craters, biomassFacility, frozenForest, ruinousShores, windsweptIslands, stainedMountains, tarFields,
    fungalPass, extractionOutpost, saltFlats, overgrowth,
    impact0078, desolateRift, nuclearComplex, planetaryTerminal,
    coastline, navalFortress,

    onset, aegis, lake, intersect, basin, atlas, split, marsh, peaks, ravine, caldera,
    stronghold, crevice, siege, crossroads, karst, origin;

    public static void load(){
        //region serpulo

        groundZero = new SectorPreset("groundZero", serpulo, 15){{
            alwaysUnlocked = true;
            addStartingItems = true;
            captureWave = 10;
            difficulty = 1;
            overrideLaunchDefaults = true;
            noLighting = true;
            startWaveTimeMultiplier = 3f;
        }};

        saltFlats = new SectorPreset("saltFlats", serpulo, 101){{
            difficulty = 5;
        }};

        frozenForest = new SectorPreset("frozenForest", serpulo, 86){{
            captureWave = 15;
            difficulty = 2;
        }};

        biomassFacility = new SectorPreset("biomassFacility", serpulo, 81){{
            captureWave = 20;
            difficulty = 3;
        }};

        craters = new SectorPreset("craters", serpulo, 18){{
            captureWave = 20;
            difficulty = 2;
        }};

        ruinousShores = new SectorPreset("ruinousShores", serpulo, 213){{
            captureWave = 30;
            difficulty = 3;
        }};

        windsweptIslands = new SectorPreset("windsweptIslands", serpulo, 246){{
            captureWave = 30;
            difficulty = 4;
        }};

        stainedMountains = new SectorPreset("stainedMountains", serpulo, 20){{
            captureWave = 30;
            difficulty = 3;
        }};

        extractionOutpost = new SectorPreset("extractionOutpost", serpulo, 165){{
            difficulty = 5;
        }};

        coastline = new SectorPreset("coastline", serpulo, 108){{
            captureWave = 30;
            difficulty = 5;
        }};

        navalFortress = new SectorPreset("navalFortress", serpulo, 216){{
            difficulty = 9;
        }};

        fungalPass = new SectorPreset("fungalPass", serpulo, 21){{
            difficulty = 4;
        }};

        overgrowth = new SectorPreset("overgrowth", serpulo, 134){{
            difficulty = 5;
        }};

        tarFields = new SectorPreset("tarFields", serpulo, 23){{
            captureWave = 40;
            difficulty = 5;
        }};

        impact0078 = new SectorPreset("impact0078", serpulo, 227){{
            captureWave = 45;
            difficulty = 7;
        }};

        desolateRift = new SectorPreset("desolateRift", serpulo, 123){{
            captureWave = 18;
            difficulty = 8;
        }};

        nuclearComplex = new SectorPreset("nuclearComplex", serpulo, 130){{
            captureWave = 50;
            difficulty = 7;
        }};

        planetaryTerminal = new SectorPreset("planetaryTerminal", serpulo, 93){{
            difficulty = 10;
            isLastSector = true;
        }};

        //endregion
        //region erekir

        onset = new SectorPreset("onset", erekir, 10){{
            addStartingItems = true;
            alwaysUnlocked = true;
            difficulty = 1;
        }};

        aegis = new SectorPreset("aegis", erekir, 88){{
            difficulty = 3;
        }};

        lake = new SectorPreset("lake", erekir, 41){{
            difficulty = 4;
        }};

        intersect = new SectorPreset("intersect", erekir, 36){{
            difficulty = 5;
            captureWave = 9;
            attackAfterWaves = true;
        }};

        atlas = new SectorPreset("atlas", erekir, 14){{ //TODO random sector, pick a better one
            difficulty = 5;
        }};

        split = new SectorPreset("split", erekir, 19){{ //TODO random sector, pick a better one
            difficulty = 2;
        }};

        basin = new SectorPreset("basin", erekir, 29){{
            difficulty = 6;
        }};

        marsh = new SectorPreset("marsh", erekir, 25){{
            difficulty = 4;
        }};

        peaks = new SectorPreset("peaks", erekir, 30){{
            difficulty = 3;
        }};

        ravine = new SectorPreset("ravine", erekir, 39){{
            difficulty = 4;
            captureWave = 24;
        }};

        caldera = new SectorPreset("caldera-erekir", erekir, 43){{
            difficulty = 4;
        }};

        stronghold = new SectorPreset("stronghold", erekir, 18){{
            difficulty = 7;
        }};

        crevice = new SectorPreset("crevice", erekir, 3){{
            difficulty = 6;
            captureWave = 46;
        }};

        siege = new SectorPreset("siege", erekir, 58){{
            difficulty = 8;
        }};

        crossroads = new SectorPreset("crossroads", erekir, 37){{
            difficulty = 7;
        }};

        karst = new SectorPreset("karst", erekir, 5){{
            difficulty = 9;
            captureWave = 10;
        }};

        origin = new SectorPreset("origin", erekir, 12){{
            difficulty = 10;
            isLastSector = true;
        }};

        //endregion
    }
}
