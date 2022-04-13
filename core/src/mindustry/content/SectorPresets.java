package mindustry.content;

import mindustry.game.MapObjectives.*;
import mindustry.type.*;

import static mindustry.content.Planets.*;

public class SectorPresets{
    public static SectorPreset
    groundZero,
    craters, biomassFacility, frozenForest, ruinousShores, windsweptIslands, stainedMountains, tarFields,
    fungalPass, extractionOutpost, saltFlats, overgrowth,
    impact0078, desolateRift, nuclearComplex, planetaryTerminal,
    coastline, navalFortress,

    onset, two, three, four, five
    ;

    public static void load(){
        //region serpulo

        groundZero = new SectorPreset("groundZero", serpulo, 15){{
            alwaysUnlocked = true;
            addStartingItems = true;
            captureWave = 10;
            difficulty = 1;
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
        }};

        //endregion
        //region erekir

        onset = new SectorPreset("onset", erekir, 10){{
            addStartingItems = true;
            alwaysUnlocked = true;
            difficulty = 1;

            rules = r -> {
                r.objectives.addAll(
                    new ItemObjective(Items.beryllium, 15).withMarkers(
                            new ShapeTextMarker("Click to mine [accent]resources[] from walls.", 1984f, 2240f),
                            new ShapeTextMarker("Move your unit with WASD.", 235.5f * 8f, 272.5f * 8f, 6f, 0f, 40f)
                    ),
                    new ResearchObjective(Blocks.turbineCondenser).withMarkers(
                            new ShapeTextMarker("Open the [accent]research tree[] and research the [accent]turbine condenser[].\nThis will allow you to build it.", 249 * 8f, 269f * 8f, 0f, 0f, 9f)
                    ),
                    new BuildCountObjective(Blocks.turbineCondenser, 1).withMarkers(
                            new ShapeTextMarker("Place a [accent]turbine condenser[] on the vent.\nThis will generate [accent]power[].", 253f * 8f, 258f * 8f, 8f * 2.6f, 0f, 9f)
                    ),
                    new BuildCountObjective(Blocks.plasmaBore, 1).withMarkers(
                            new ShapeTextMarker("Research and place a [accent]plasma bore[]. \nIt automatically mines resources from walls.", 253.5f * 8f, 275.5f * 8f, 4f * 2.6f, 45f, 60f)
                    ),
                    new BuildCountObjective(Blocks.beamNode, 1).withMarkers(
                            new ShapeTextMarker("To [accent]power[] the plasma bore, research and place a [accent]beam node[].\nConnect the turbine condenser to the plasma bore.", 254f * 8f, 267f * 8f)
                    ),
                    new CoreItemObjective(Items.beryllium, 5).withMarkers(
                            new ShapeTextMarker("Once the plasma bore is powered, the next step is to route the resources to the core.\nUnlock and place [accent]ducts[] to move the mined resources to the core", 244f * 8f, 274f * 8f, 1f, 0f, 75f)
                    ),
                    new CoreItemObjective(Items.beryllium, 200).withMarkers(
                            new ShapeTextMarker("Expand your mining operation!\nPlace more plasma bores and use beam nodes and ducts to support them.", 253f * 8f, 282f * 8f, 0f, 0f, 30f),
                            new ShapeTextMarker("Try to collect 200 beryllium.", 236f * 8f, 276 * 8f, 0f, 0f, 9f)
                    ),
                    new CoreItemObjective(Items.graphite, 100)
                );
            };
        }};

        two = new SectorPreset("two", erekir, 88){{
            difficulty = 3;
        }};

        three = new SectorPreset("three", erekir, 36){{
            difficulty = 5;
        }};

        four = new SectorPreset("four", erekir, 29){{
            difficulty = 6;
        }};

        five = new SectorPreset("five", erekir, 12){{
            difficulty = 7;
        }};

        //endregion
    }
}
