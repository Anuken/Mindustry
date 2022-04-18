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
                        new ShapeTextMarker("Click to mine [accent]resources[] from walls.", 290f * 8f, 106f * 8f),
                        new TextMarker("Move your unit with WASD.", 276.5f * 8f, 112.5f * 8f)
                    ),
                    new ResearchObjective(Blocks.turbineCondenser).withMarkers(
                        new TextMarker("Open the [accent]research tree[] and research the [accent]turbine condenser[].\nThis will allow you to build it.", 280f * 8f, 118f * 8f)
                    ),
                    new BuildCountObjective(Blocks.turbineCondenser, 1).withMarkers(
                        new ShapeTextMarker("Place a [accent]turbine condenser[] on the vent.\nThis will generate [accent]power[].", 289f * 8f, 116f * 8f, 8f * 2.6f, 0f, 9f)
                    ),
                    new BuildCountObjective(Blocks.plasmaBore, 1).withMarkers(
                        new ShapeTextMarker("Research and place a [accent]plasma bore[]. \nIt automatically mines resources from walls.", 293.5f * 8f, 113.5f * 8f, 4f * 2.6f, 45f, 60f)
                    ),
                    new BuildCountObjective(Blocks.beamNode, 1).withMarkers(
                        new ShapeTextMarker("To [accent]power[] the plasma bore, research and place a [accent]beam node[].\nConnect the turbine condenser to the plasma bore.", 294f * 8f, 116f * 8f)
                    ),
                    new CoreItemObjective(Items.beryllium, 5).withMarkers(
                        new TextMarker("Once the plasma bore is powered, the next step is to route the resources to the core.\nResearch and place [accent]ducts[] to move the mined resources to the core.", 285f * 8f, 108f * 8f)
                    ),
                    new CoreItemObjective(Items.beryllium, 200).withMarkers(
                        new TextMarker("Expand your mining operation!\nPlace more plasma bores and use beam nodes and ducts to support them.\nMine 200 beryllium.", 280f * 8f, 118f * 8f)
                    ),
                    new CoreItemObjective(Items.graphite, 100).withMarkers(
                        new TextMarker("More complex blocks require [accent]graphite[].]\nTry setting up mining for graphite.", 261f * 8f, 108f * 8f)
                    ),
                    new ResearchObjective(Blocks.siliconArcFurnace).withMarkers(
                        new TextMarker("Time to expand into [accent]factories[]!\nResearch [accent]cliff crusher[] and [accent]silicon arc furnace[].", 268f * 8f, 101f * 8f)
                    ),
                    new CoreItemObjective(Items.silicon, 50).withMarkers(
                        new TextMarker("The arc furnace needs both [accent]sand[] and [accent]graphite[] to create [accent]silicon[].\nIt takes [accent]power[], too.", 268f * 8f, 101f * 8f),
                        new TextMarker("Use [accent]cliff crushers[] to mine sand.", 262f * 8f, 88f * 8f)
                    ),
                    new BuildCountObjective(Blocks.fabricator, 1).withMarkers(
                        new TextMarker("Use [accent]units[] to explore the map, defend your buildings, and go on the offensive.\n Research and place the [accent]fabricator[]", 258f * 8f, 116f * 8f)
                    ),
                    new UnitCountObjective(UnitTypes.stell, 1).withMarkers(
                        new TextMarker("Produce a unit.\nUse the \"?\" button to see selected factory requirements.", 258f * 8f, 116f * 8f)
                    ),
                    new CommandModeObjective().withMarkers(
                        new TextMarker("Hold [accent]shift[] to enter [accent]command mode[].\n[accent]Left-click and drag[] to select units.\n[accent]Right-click[] to order your selected units to move or attack.", 258f * 8f, 116f * 8f)
                    ),
                    new BuildCountObjective(Blocks.breach, 1).withMarkers(
                            new TextMarker("Units are effective, but [accent]turrets[] provide better defensive capabilities if used effectively.\n Place a [accent]Breach[] turret.\nTurrets require [accent]ammo[].", 258f * 8f, 114f * 8f)
                    ),
                    new BuildCountObjective(Blocks.berylliumWall, 6).withMarkers(
                            new TextMarker("[accent]Walls[] can prevent oncoming damage from reaching your buildings\nPlace some [accent]beryllium walls[] around the turret.", 276f * 8f, 133f * 8f)
                    ),
                    new TimerObjective("@objective.enemiesapproaching",30 * 60).withMarkers(
                            new TextMarker("Enemy incoming, prepare to defend.", 276f * 8f, 133f * 8f)
                    ).withFlags("defStart"),
                    new DestroyUnitsObjective(2).withFlags("defDone")
                );
            };
        }};

        two = new SectorPreset("two", erekir, 88){{
            difficulty = 3;

            rules = r -> {
                r.objectives.addAll(
                        new UnitCountObjective(UnitTypes.stell, 2).withMarkers(
                                new TextMarker("The enemy will attack soon.\n Build units to defend your core.", 276f * 8f, 133f * 8f)
                        ),
                        new BuildCountObjective(Blocks.breach, 1).withMarkers(
                                new TextMarker("Units are effective, but [accent]turrets[] provide better defensive capabilities if used effectively.\n Place a [accent]Breach[] turret.\nTurrets require [accent]ammo[].", 276f * 8f, 133f * 8f)
                        ).withFlags("defDone"),
                        new BuildCountObjective(Blocks.berylliumWall, 6).withMarkers(
                                new TextMarker("[accent]Walls[] can prevent oncoming damage from reaching your buildings\nPlace some [accent]beryllium walls[] around the turret.", 276f * 8f, 133f * 8f)
                        ),
                        new BuildCountObjective(Blocks.coreBastion, 1).withMarkers(
                                new TextMarker("You must expand.\nBuild more units and go on the offensive", 276f * 8f, 133f * 8f),
                                new TextMarker("Cores can only be placed in special zone tiles.", 287 * 8f, 201 * 8f)
                        ).withFlags("def2Done")
                );
            };
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
