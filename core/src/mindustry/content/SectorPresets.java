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

    onset, two, lake, three, four
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
        }};

        two = new SectorPreset("two", erekir, 88){{
            difficulty = 3;

            /*rules = r -> r.objectives.add(
                new TimerObjective("[lightgray]Enemy detection:[] [accent]{0}", 7 * 60 * 60).markers(
                    new TextMarker("The enemy will begin constructing units in 7 minutes.", 276f * 8f, 164f * 8f)
                ).flagsAdded("beginBuilding")
                .child(new ProduceObjective(Items.tungsten).markers(
                    new ShapeTextMarker("Tungsten can be mined using an [accent]impact drill[].\nThis structure requires [accent]water[] and [accent]power[].", 220f * 8f, 181f * 8f)
                ).child(new DestroyBlockObjective(Blocks.largeShieldProjector, 210, 278, Team.malis).markers(
                    new TextMarker("The enemy is protected by shields.\nAn experimental shield breaker module has been detected in this sector.\nFind and activate it using tungsten.", 276f * 8f, 164f * 8f),
                    new MinimapMarker(23, 137, Pal.accent)
                )))
            );*/
        }};

        lake = new SectorPreset("lake", erekir, 41){{
            difficulty = 4;

        }};

        three = new SectorPreset("three", erekir, 36){{
            difficulty = 5;

            captureWave = 9;
        }};

        four = new SectorPreset("four", erekir, 29){{
            difficulty = 6;

            /*rules = r -> {
                float rad = 52f;
                r.objectives.add(
                    new DestroyBlocksObjective(Blocks.coreBastion, Team.malis, new Point2(290, 501), new Point2(158, 496))
                    .flagsAdded("nukeannounce")
                    .child(new TimerObjective("@objective.nuclearlaunch", 8 * 60 * 60).markers(
                        new MinimapMarker(338, 377, rad, 14f, Pal.remove),
                        new ShapeMarker(338 * 8, 377 * 8f){{
                            radius = rad * 8f;
                            fill = true;
                            color = Pal.remove.cpy().mul(0.8f).a(0.3f);
                            sides = 90;
                        }},
                        new ShapeMarker(338 * 8, 377 * 8f){{
                            radius = rad * 8f;
                            color = Pal.remove;
                            sides = 90;
                        }}
                    ).flagsAdded("nuke1"))
                );
            };*/
        }};

        //endregion
    }
}
