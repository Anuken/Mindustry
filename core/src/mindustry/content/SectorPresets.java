package mindustry.content;

import mindustry.maps.*;
import mindustry.type.*;

import static mindustry.content.Planets.*;

public class SectorPresets{
    public static SectorPreset
    groundZero,
    crateredBattleground, biomassFacility, taintedWoods, frozenForest, ruinousShores, facility32m, windsweptIslands, stainedMountains, tarFields,
    frontier, fungalPass, infestedCanyons, atolls, sunkenPier, mycelialBastion, extractionOutpost, saltFlats, testingGrounds, overgrowth,
    impact0078, desolateRift, nuclearComplex, planetaryTerminal,
    coastline, navalFortress, weatheredChannels, perilousHarbor, littoralShipyard,

    onset, aegis, lake, intersect, basin, atlas, split, marsh, peaks, ravine, caldera,
    stronghold, crevice, siege, crossroads, karst, origin;

    public static void load(){
        //region serpulo

        groundZero = new SectorPreset("groundZero", serpulo, 15){{
            alwaysUnlocked = true;
            addStartingItems = true;
            captureWave = 10;
            threat = SectorThreat.low;
            overrideLaunchDefaults = true;
            noLighting = true;
            startWaveTimeMultiplier = 3f;
        }};

        saltFlats = new SectorPreset("saltFlats", serpulo, 101){{
            threat = SectorThreat.high;
        }};

        testingGrounds = new SectorPreset("testingGrounds", serpulo, 3){{
            threat = SectorThreat.high;
            captureWave = 33;
        }};

        frozenForest = new SectorPreset("frozenForest", serpulo, 86){{
            captureWave = 15;
            threat = SectorThreat.low;
        }};

        biomassFacility = new SectorPreset("biomassFacility", serpulo, 81){{
            captureWave = 20;
            threat = SectorThreat.medium;
        }};

        taintedWoods = new SectorPreset("taintedWoods", serpulo, 221){{
            captureWave = 33;
            threat = SectorThreat.high;
        }};

        crateredBattleground = new SectorPreset("crateredBattleground", serpulo, 18){{
            captureWave = 20;
            threat = SectorThreat.low;
        }};

        ruinousShores = new SectorPreset("ruinousShores", serpulo, 213){{
            captureWave = 30;
            threat = SectorThreat.medium;
        }};

        perilousHarbor = new SectorPreset("perilousHarbor", serpulo, 47){{
            threat = SectorThreat.medium;
        }};

        facility32m = new SectorPreset("facility32m", serpulo, 64){{
            captureWave = 25;
            threat = SectorThreat.medium;
        }};

        windsweptIslands = new SectorPreset("windsweptIslands", serpulo, 246){{
            captureWave = 30;
            threat = SectorThreat.medium;
        }};

        stainedMountains = new SectorPreset("stainedMountains", serpulo, 20){{
            captureWave = 30;
            threat = SectorThreat.medium;
        }};

        extractionOutpost = new SectorPreset("extractionOutpost", serpulo, 165){{
            threat = SectorThreat.high;
        }};

        coastline = new SectorPreset("coastline", serpulo, 108){{
            captureWave = 30;
            threat = SectorThreat.high;
        }};

        weatheredChannels = new SectorPreset("weatheredChannels", serpulo, 39){{
            captureWave = 40;
            threat = SectorThreat.extreme;
        }};

        navalFortress = new SectorPreset("navalFortress", serpulo, 216){{
            threat = SectorThreat.extreme;
        }};

        frontier = new SectorPreset("frontier", serpulo, 50){{
            threat = SectorThreat.medium;
        }};

        fungalPass = new SectorPreset("fungalPass", serpulo, 21){{
            threat = SectorThreat.low;
        }};

        infestedCanyons = new SectorPreset("infestedCanyons", serpulo, 210){{
            threat = SectorThreat.medium;
        }};

        atolls = new SectorPreset("atolls", serpulo, 1){{
            threat = SectorThreat.high;
        }};

        sunkenPier = new SectorPreset("sunkenPier", serpulo, -1){{
            captureWave = 50;
            threat = SectorThreat.extreme;
        }};

        mycelialBastion = new SectorPreset("mycelialBastion", serpulo, 260){{
            threat = SectorThreat.extreme;
        }};

        overgrowth = new SectorPreset("overgrowth", serpulo, 134){{
            threat = SectorThreat.high;
        }};

        tarFields = new SectorPreset("tarFields", serpulo, 23){{
            captureWave = 40;
            threat = SectorThreat.high;
        }};

        impact0078 = new SectorPreset("impact0078", serpulo, 227){{
            captureWave = 45;
            threat = SectorThreat.high;
        }};

        desolateRift = new SectorPreset("desolateRift", serpulo, 123){{
            captureWave = 18;
            threat = SectorThreat.extreme;
        }};

        nuclearComplex = new SectorPreset("nuclearComplex", serpulo, 130){{
            captureWave = 50;
            threat = SectorThreat.high;
        }};

        littoralShipyard = new SectorPreset("littoralShipyard", serpulo, 204){{
            threat = SectorThreat.extreme;
        }};

        planetaryTerminal = new SectorPreset("planetaryTerminal", serpulo, 93){{
            threat = SectorThreat.eradication;
            isLastSector = true;
        }};

        SectorSubmissions.registerSectors();

        //endregion
        //region erekir

        onset = new SectorPreset("onset", erekir, 10){{
            alwaysUnlocked = true;
            threat = SectorThreat.low;
        }};

        aegis = new SectorPreset("aegis", erekir, 88){{
            threat = SectorThreat.medium;
        }};

        lake = new SectorPreset("lake", erekir, 41){{
            threat = SectorThreat.medium;
        }};

        intersect = new SectorPreset("intersect", erekir, 36){{
            threat = SectorThreat.high;
            captureWave = 9;
            attackAfterWaves = true;
        }};

        atlas = new SectorPreset("atlas", erekir, 14){{
            threat = SectorThreat.high;
        }};

        split = new SectorPreset("split", erekir, 19){{
            threat = SectorThreat.low;
        }};

        basin = new SectorPreset("basin", erekir, 29){{
            threat = SectorThreat.high;
        }};

        marsh = new SectorPreset("marsh", erekir, 25){{
            threat = SectorThreat.medium;
        }};

        peaks = new SectorPreset("peaks", erekir, 30){{
            threat = SectorThreat.medium;
        }};

        ravine = new SectorPreset("ravine", erekir, 39){{
            threat = SectorThreat.medium;
            captureWave = 24;
        }};

        caldera = new SectorPreset("caldera-erekir", erekir, 43){{
            threat = SectorThreat.medium;
        }};

        stronghold = new SectorPreset("stronghold", erekir, 18){{
            threat = SectorThreat.high;
        }};

        crevice = new SectorPreset("crevice", erekir, 3){{
            threat = SectorThreat.high;
            captureWave = 46;
        }};

        siege = new SectorPreset("siege", erekir, 58){{
            threat = SectorThreat.extreme;
        }};

        crossroads = new SectorPreset("crossroads", erekir, 37){{
            threat = SectorThreat.high;
        }};

        karst = new SectorPreset("karst", erekir, 5){{
            threat = SectorThreat.extreme;
            captureWave = 10;
        }};

        origin = new SectorPreset("origin", erekir, 12){{
            threat = SectorThreat.eradication;
            isLastSector = true;
        }};

        //endregion
    }
}