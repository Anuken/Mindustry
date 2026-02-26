package mindustry.content;

import mindustry.maps.*;
import mindustry.type.*;

import static mindustry.content.Planets.*;

public class SectorPresets{
    public static SectorPreset
    groundZero,
    craters, biomassFacility, taintedWoods, frozenForest, ruinousShores, facility32m, windsweptIslands, stainedMountains, tarFields,
    frontier, fungalPass, infestedCanyons, atolls, sunkenPier, mycelialBastion, extractionOutpost, saltFlats, testingGrounds, overgrowth,
    impact0078, desolateRift, fallenVessel, nuclearComplex, planetaryTerminal,
    coastline, navalFortress, weatheredChannels, perilousHarbor,

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

        testingGrounds = new SectorPreset("testingGrounds", serpulo, 3){{
            difficulty = 7;
            captureWave = 33;
        }};

        frozenForest = new SectorPreset("frozenForest", serpulo, 86){{
            captureWave = 15;
            difficulty = 2;
        }};

        biomassFacility = new SectorPreset("biomassFacility", serpulo, 81){{
            captureWave = 20;
            difficulty = 3;
        }};

        taintedWoods = new SectorPreset("taintedWoods", serpulo, 221){{
            captureWave = 33;
            difficulty = 5;
        }};

        craters = new SectorPreset("craters", serpulo, 18){{
            captureWave = 20;
            difficulty = 2;
        }};

        ruinousShores = new SectorPreset("ruinousShores", serpulo, 213){{
            captureWave = 30;
            difficulty = 3;
        }};

        perilousHarbor = new SectorPreset("perilousHarbor", serpulo, 47){{
            difficulty = 4;
        }};

        facility32m = new SectorPreset("facility32m", serpulo, 64){{
            captureWave = 25;
            difficulty = 4;
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

        weatheredChannels = new SectorPreset("weatheredChannels", serpulo, 39){{
            captureWave = 40;
            difficulty = 9;
        }};

        navalFortress = new SectorPreset("navalFortress", serpulo, 216){{
            difficulty = 8;
        }};

        frontier = new SectorPreset("frontier", serpulo, 50){{
            difficulty = 4;
        }};

        fungalPass = new SectorPreset("fungalPass", serpulo, 21){{
            difficulty = 2;
        }};

        infestedCanyons = new SectorPreset("infestedCanyons", serpulo, 210){{
            difficulty = 4;
        }};

        atolls = new SectorPreset("atolls", serpulo, 1){{
            difficulty = 7;
        }};

        sunkenPier = new SectorPreset("sunkenPier", serpulo, -1){{
            captureWave = 50;
            difficulty = 8;
        }};

        mycelialBastion = new SectorPreset("mycelialBastion", serpulo, 260){{
            difficulty = 8;
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

        fallenVessel = new SectorPreset("fallenVessel", serpulo, -1){{
            captureWave = 70;
            difficulty = 9;
        }};

        nuclearComplex = new SectorPreset("nuclearComplex", serpulo, 130){{
            captureWave = 50;
            difficulty = 7;
        }};

        planetaryTerminal = new SectorPreset("planetaryTerminal", serpulo, 93){{
            difficulty = 10;
            isLastSector = true;
        }};

        new SectorPreset("geothermalStronghold", serpulo, 264){{
            requireUnlock = false;
            difficulty = 10;
            showHidden = true;
        }};

        new SectorPreset("cruxscape", serpulo, 54){{
            requireUnlock = false;
            difficulty = 10;
            showHidden = true;
        }};

        SectorSubmissions.registerSectors();

        //endregion
        //region erekir

        onset = new SectorPreset("onset", erekir, 10){{
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

        atlas = new SectorPreset("atlas", erekir, 14){{
            difficulty = 5;
        }};

        split = new SectorPreset("split", erekir, 19){{
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
