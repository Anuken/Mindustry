package io.anuke.mindustry.content;

import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Objectives.*;
import io.anuke.mindustry.maps.generators.*;
import io.anuke.mindustry.maps.generators.MapGenerator.*;
import io.anuke.mindustry.maps.zonegen.*;
import io.anuke.mindustry.type.*;

import static io.anuke.arc.collection.Array.with;
import static io.anuke.mindustry.content.Items.*;
import static io.anuke.mindustry.type.ItemStack.list;

public class Zones implements ContentList{
    public static Zone
    groundZero, desertWastes,
    craters, frozenForest, ruinousShores, stainedMountains, tarFields, fungalPass,
    saltFlats, overgrowth, impact0078, crags,
    desolateRift, nuclearComplex;

    @Override
    public void load(){

        groundZero = new Zone("groundZero", new MapGenerator("groundZero", 1)){{
            baseLaunchCost = list(copper, -60);
            startingItems = list(copper, 60);
            alwaysUnlocked = true;
            conditionWave = 5;
            launchPeriod = 5;
            resources = with(copper, scrap, lead);
        }};

        desertWastes = new Zone("desertWastes", new DesertWastesGenerator(260, 260)){{
            startingItems = list(copper, 120);
            conditionWave = 20;
            launchPeriod = 10;
            loadout = Loadouts.advancedShard;
            resources = with(copper, lead, coal, sand);
            rules = r -> {
                r.waves = true;
                r.waveTimer = true;
                r.launchWaveMultiplier = 3f;
                r.waveSpacing = 60 * 50f;
                r.spawns = with(
                    new SpawnGroup(UnitTypes.crawler){{
                        unitScaling = 3f;
                    }},
                    new SpawnGroup(UnitTypes.dagger){{
                        unitScaling = 4f;
                        begin = 2;
                        spacing = 2;
                    }},
                    new SpawnGroup(UnitTypes.wraith){{
                        unitScaling = 3f;
                        begin = 11;
                        spacing = 3;
                    }},
                    new SpawnGroup(UnitTypes.eruptor){{
                        unitScaling = 3f;
                        begin = 22;
                        unitAmount = 1;
                        spacing = 4;
                    }},
                    new SpawnGroup(UnitTypes.titan){{
                        unitScaling = 3f;
                        begin = 37;
                        unitAmount = 2;
                        spacing = 4;
                    }},
                    new SpawnGroup(UnitTypes.fortress){{
                        unitScaling = 2f;
                        effect = StatusEffects.boss;
                        begin = 41;
                        spacing = 20;
                    }}
                );
            };
            requirements = with(
            new ZoneWave(groundZero, 20),
            new Unlock(Blocks.combustionGenerator)
            );
        }};

        saltFlats = new Zone("saltFlats", new MapGenerator("saltFlats")){{
            startingItems = list(copper, 200, Items.silicon, 200, lead, 200);
            loadout = Loadouts.basicFoundation;
            conditionWave = 10;
            launchPeriod = 5;
            configureObjective = new Launched(this);
            resources = with(copper, scrap, lead, coal, sand, titanium);
            requirements = with(
            new ZoneWave(desertWastes, 60),
            new Unlock(Blocks.daggerFactory),
            new Unlock(Blocks.draugFactory),
            new Unlock(Blocks.door),
            new Unlock(Blocks.waterExtractor)
            );
        }};

        frozenForest = new Zone("frozenForest", new MapGenerator("frozenForest", 1)
        .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.02))){{
            loadout = Loadouts.basicFoundation;
            startingItems = list(copper, 250);
            conditionWave = 10;
            resources = with(copper, lead, coal);
            requirements = with(
            new ZoneWave(groundZero, 10),
            new Unlock(Blocks.junction),
            new Unlock(Blocks.router)
            );
        }};

        craters = new Zone("craters", new MapGenerator("craters", 1).decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.004))){{
            startingItems = list(copper, 100);
            conditionWave = 10;
            resources = with(copper, lead, coal, sand, scrap);
            requirements = with(
            new ZoneWave(frozenForest, 10),
            new Unlock(Blocks.mender),
            new Unlock(Blocks.combustionGenerator)
            );
        }};

        ruinousShores = new Zone("ruinousShores", new MapGenerator("ruinousShores", 1)){{
            loadout = Loadouts.basicFoundation;
            startingItems = list(copper, 140, lead, 50);
            conditionWave = 20;
            launchPeriod = 20;
            resources = with(copper, scrap, lead, coal, sand);
            requirements = with(
            new ZoneWave(desertWastes, 20),
            new ZoneWave(craters, 15),
            new Unlock(Blocks.graphitePress),
            new Unlock(Blocks.combustionGenerator),
            new Unlock(Blocks.kiln),
            new Unlock(Blocks.mechanicalPump)
            );
        }};

        stainedMountains = new Zone("stainedMountains", new MapGenerator("stainedMountains", 2)
        .decor(new Decoration(Blocks.shale, Blocks.shaleBoulder, 0.02))){{
            loadout = Loadouts.basicFoundation;
            startingItems = list(copper, 200, lead, 50);
            conditionWave = 10;
            launchPeriod = 10;
            resources = with(copper, scrap, lead, coal, titanium, sand);
            requirements = with(
            new ZoneWave(frozenForest, 15),
            new Unlock(Blocks.pneumaticDrill),
            new Unlock(Blocks.powerNode),
            new Unlock(Blocks.turbineGenerator)
            );
        }};

        fungalPass = new Zone("fungalPass", new MapGenerator("fungalPass")){{
            startingItems = list(copper, 250, lead, 250, Items.metaglass, 100, Items.graphite, 100);
            resources = with(copper, lead, coal, titanium, sand);
            configureObjective = new Launched(this);
            requirements = with(
            new ZoneWave(stainedMountains, 15),
            new Unlock(Blocks.daggerFactory),
            new Unlock(Blocks.crawlerFactory),
            new Unlock(Blocks.door),
            new Unlock(Blocks.siliconSmelter)
            );
        }};

        overgrowth = new Zone("overgrowth", new MapGenerator("overgrowth")){{
            startingItems = list(copper, 1500, lead, 1000, Items.silicon, 500, Items.metaglass, 250);
            conditionWave = 12;
            launchPeriod = 4;
            loadout = Loadouts.basicNucleus;
            configureObjective = new Launched(this);
            resources = with(copper, lead, coal, titanium, sand, thorium, scrap);
            requirements = with(
            new ZoneWave(craters, 40),
            new Launched(fungalPass),
            new Unlock(Blocks.cultivator),
            new Unlock(Blocks.sporePress),
            new Unlock(Blocks.titanFactory),
            new Unlock(Blocks.wraithFactory)
            );
        }};

        tarFields = new Zone("tarFields", new MapGenerator("tarFields")
        .decor(new Decoration(Blocks.shale, Blocks.shaleBoulder, 0.02))){{
            loadout = Loadouts.basicFoundation;
            startingItems = list(copper, 250, lead, 100);
            conditionWave = 15;
            launchPeriod = 10;
            requirements = with(new ZoneWave(ruinousShores, 20));
            resources = with(copper, scrap, lead, coal, titanium, thorium, sand);
            requirements = with(
            new ZoneWave(ruinousShores, 20),
            new Unlock(Blocks.coalCentrifuge),
            new Unlock(Blocks.conduit),
            new Unlock(Blocks.wave)
            );
        }};

        desolateRift = new Zone("desolateRift", new MapGenerator("desolateRift")){{
            loadout = Loadouts.basicNucleus;
            startingItems = list(copper, 1000, lead, 1000, Items.graphite, 250, titanium, 250, Items.silicon, 250);
            conditionWave = 3;
            launchPeriod = 2;
            resources = with(copper, scrap, lead, coal, titanium, sand, thorium);
            requirements = with(
            new ZoneWave(tarFields, 20),
            new Unlock(Blocks.thermalGenerator),
            new Unlock(Blocks.thoriumReactor)
            );
        }};

        /*
        crags = new Zone("crags", new MapGenerator("crags").dist(2f)){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 2000, Items.lead, 2000, Items.graphite, 500, Items.titanium, 500, Items.silicon, 500);
            conditionWave = 3;
            launchPeriod = 2;
            requirements = with(stainedMountains, 40);
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            resources = Array.with(Items.copper, Items.scrap, Items.lead, Items.coal, Items.sand};
        }};*/

        nuclearComplex = new Zone("nuclearComplex", new MapGenerator("nuclearProductionComplex", 1)
        .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.01))){{
            loadout = Loadouts.basicNucleus;
            startingItems = list(copper, 1250, lead, 1500, Items.silicon, 400, Items.metaglass, 250);
            conditionWave = 30;
            launchPeriod = 15;
            resources = with(copper, scrap, lead, coal, titanium, thorium, sand);
            requirements = with(
            new Launched(fungalPass),
            new Unlock(Blocks.thermalGenerator),
            new Unlock(Blocks.laserDrill)
            );
        }};

        /*
        impact0078 = new Zone("impact0078", new MapGenerator("impact0078").dist(2f)){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 2000, Items.lead, 2000, Items.graphite, 500, Items.titanium, 500, Items.silicon, 500);
            conditionWave = 3;
            launchPeriod = 2;
            requirements = with(nuclearComplex, 40);
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            resources = Array.with(Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.thorium};
        }};*/
    }
}
