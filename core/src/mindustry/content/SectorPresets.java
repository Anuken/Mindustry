package mindustry.content;

import arc.math.geom.*;
import mindustry.game.*;
import mindustry.game.MapObjectives.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static mindustry.content.Planets.*;

public class SectorPresets{
    public static SectorPreset
    groundZero,
    craters, biomassFacility, frozenForest, ruinousShores, windsweptIslands, stainedMountains, tarFields,
    fungalPass, extractionOutpost, saltFlats, overgrowth,
    impact0078, desolateRift, nuclearComplex, planetaryTerminal,
    coastline, navalFortress,

    onset, two, lake, three, four, atlas, split;

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
        }};

        two = new SectorPreset("two", erekir, 88){{
            difficulty = 3;
        }};

        lake = new SectorPreset("lake", erekir, 41){{
            difficulty = 4;

            rules = r -> {
                r.objectives.addAll(
                    new BuildCountObjective(Blocks.shipFabricator, 1),
                    new UnitCountObjective(UnitTypes.elude, 1)
                );
            };
        }};

        three = new SectorPreset("three", erekir, 36){{
            difficulty = 5;

            captureWave = 9;
        }};

        atlas = new SectorPreset("atlas", erekir, 14){{ //TODO random sector, pick a better one
            difficulty = 5;
        }};

        split = new SectorPreset("split", erekir, 19){{ //TODO random sector, pick a better one
            difficulty = 5;

            rules = r -> {
                r.objectives.addAll(
                    new CoreItemObjective(Items.tungsten, 100).withMarkers(
                        new TextMarker("Some blocks can be picked up by the core unit.\nPick up this [accent]container[] and place it onto the [accent]payload loader[].\n(Default keys are [ and ] to pick up and drop)", 347 * 8f, 445f * 8f),
                        new TextMarker("You must acquire some tungsten to build units.", 293 * 8f, 417 * 8f)
                    ),
                    new BuildCountObjective(Blocks.payloadMassDriver, 2).withMarkers(
                        new TextMarker("Units must be transported to the other side of the wall.\nPlace two [accent]Payload Mass Drivers[], one on each side of the wall.\nSet up the link by pressing one of them, then selecting the other.", 293 * 8f, 417 * 8f)
                    ),
                    new DestroyCoreObjective().withMarkers(
                        new TextMarker("Similar to the container, units can also be transported using a [accent]Payload Mass Driver[].\nPlace a unit fabricator adjacent to a mass driver to load them, then send them across the wall to attack the enemy base.", 293 * 8f, 417 * 8f)
                    )
                );
            };
        }};

        four = new SectorPreset("four", erekir, 29){{
            difficulty = 6;
        }};

        //endregion
    }
}
