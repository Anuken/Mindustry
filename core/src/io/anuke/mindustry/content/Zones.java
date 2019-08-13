package io.anuke.mindustry.content;

import io.anuke.arc.collection.Array;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.maps.generators.MapGenerator;
import io.anuke.mindustry.maps.generators.MapGenerator.Decoration;
import io.anuke.mindustry.maps.zonegen.DesertWastesGenerator;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;

public class Zones implements ContentList{
    public static Zone
    groundZero, desertWastes,
    craters, frozenForest, ruinousShores, stainedMountains, tarFields, fungalPass,
    saltFlats, overgrowth, impact0078, crags,
    desolateRift, nuclearComplex;

    @Override
    public void load(){

        groundZero = new Zone("groundZero", new MapGenerator("groundZero", 1)){{
            baseLaunchCost = ItemStack.with(Items.copper, -60);
            startingItems = ItemStack.list(Items.copper, 60);
            alwaysUnlocked = true;
            conditionWave = 5;
            launchPeriod = 5;
            resources = new Item[]{Items.copper, Items.scrap, Items.lead};
        }};

        desertWastes = new Zone("desertWastes", new DesertWastesGenerator(260, 260)){{
            startingItems = ItemStack.list(Items.copper, 120);
            conditionWave = 20;
            launchPeriod = 10;
            loadout = Loadouts.advancedShard;
            zoneRequirements = ZoneRequirement.with(groundZero, 20);
            blockRequirements = new Block[]{Blocks.combustionGenerator};
            resources = new Item[]{Items.copper, Items.lead, Items.coal, Items.sand};
            rules = r -> {
                r.waves = true;
                r.waveTimer = true;
                r.launchWaveMultiplier = 3f;
                r.waveSpacing = 60 * 50f;
                r.spawns = Array.with(
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
        }};

        saltFlats = new Zone("saltFlats", new MapGenerator("saltFlats")){{
            startingItems = ItemStack.list(Items.copper, 200, Items.silicon, 200, Items.lead, 200);
            loadout = Loadouts.basicFoundation;
            conditionWave = 10;
            launchPeriod = 5;
            zoneRequirements = ZoneRequirement.with(desertWastes, 60);
            blockRequirements = new Block[]{Blocks.daggerFactory, Blocks.draugFactory, Blocks.door, Blocks.waterExtractor};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.sand, Items.titanium};
        }};

        frozenForest = new Zone("frozenForest", new MapGenerator("frozenForest", 1)
        .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.02))){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 250);
            conditionWave = 10;
            blockRequirements = new Block[]{Blocks.junction, Blocks.router};
            zoneRequirements = ZoneRequirement.with(groundZero, 10);
            resources = new Item[]{Items.copper, Items.lead, Items.coal};
        }};

        craters = new Zone("craters", new MapGenerator("craters", 1).decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.004))){{
            startingItems = ItemStack.list(Items.copper, 100);
            conditionWave = 10;
            zoneRequirements = ZoneRequirement.with(frozenForest, 10);
            blockRequirements = new Block[]{Blocks.mender, Blocks.combustionGenerator};
            resources = new Item[]{Items.copper, Items.lead, Items.coal, Items.sand, Items.scrap};
        }};

        ruinousShores = new Zone("ruinousShores", new MapGenerator("ruinousShores", 1)){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 140, Items.lead, 50);
            conditionWave = 20;
            launchPeriod = 20;
            zoneRequirements = ZoneRequirement.with(desertWastes, 20, craters, 15);
            blockRequirements = new Block[]{Blocks.graphitePress, Blocks.combustionGenerator, Blocks.kiln, Blocks.mechanicalPump};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.sand};
        }};

        stainedMountains = new Zone("stainedMountains", new MapGenerator("stainedMountains", 2)
        .decor(new Decoration(Blocks.shale, Blocks.shaleBoulder, 0.02))){{
            loadout = Loadouts.basicFoundation;
            startingItems = ItemStack.list(Items.copper, 200, Items.lead, 50);
            conditionWave = 10;
            launchPeriod = 10;
            zoneRequirements = ZoneRequirement.with(frozenForest, 15);
            blockRequirements = new Block[]{Blocks.pneumaticDrill, Blocks.powerNode, Blocks.turbineGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.sand};
        }};

        fungalPass = new Zone("fungalPass", new MapGenerator("fungalPass")){{
            startingItems = ItemStack.list(Items.copper, 250, Items.lead, 250, Items.metaglass, 100, Items.graphite, 100);
            zoneRequirements = ZoneRequirement.with(stainedMountains, 15);
            blockRequirements = new Block[]{Blocks.daggerFactory, Blocks.crawlerFactory, Blocks.door, Blocks.siliconSmelter};
            resources = new Item[]{Items.copper, Items.lead, Items.coal, Items.titanium, Items.sand};
        }};

        overgrowth = new Zone("overgrowth", new MapGenerator("overgrowth")){{
            startingItems = ItemStack.list(Items.copper, 1500, Items.lead, 1000, Items.silicon, 500, Items.metaglass, 250);
            conditionWave = 12;
            launchPeriod = 4;
            loadout = Loadouts.basicNucleus;
            zoneRequirements = ZoneRequirement.with(craters, 40, fungalPass, 10);
            blockRequirements = new Block[]{Blocks.cultivator, Blocks.sporePress, Blocks.titanFactory, Blocks.wraithFactory};
            resources = new Item[]{Items.copper, Items.lead, Items.coal, Items.titanium, Items.sand, Items.thorium, Items.scrap};
        }};

        tarFields = new Zone("tarFields", new MapGenerator("tarFields")
        .decor(new Decoration(Blocks.shale, Blocks.shaleBoulder, 0.02))){{
            loadout = Loadouts.basicFoundation;
            startingItems = ItemStack.list(Items.copper, 250, Items.lead, 100);
            conditionWave = 15;
            launchPeriod = 10;
            zoneRequirements = ZoneRequirement.with(ruinousShores, 20);
            blockRequirements = new Block[]{Blocks.coalCentrifuge, Blocks.conduit, Blocks.wave};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.thorium, Items.sand};
        }};

        desolateRift = new Zone("desolateRift", new MapGenerator("desolateRift")){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 1000, Items.lead, 1000, Items.graphite, 250, Items.titanium, 250, Items.silicon, 250);
            conditionWave = 3;
            launchPeriod = 2;
            zoneRequirements = ZoneRequirement.with(tarFields, 20);
            blockRequirements = new Block[]{Blocks.thermalGenerator, Blocks.thoriumReactor};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.sand, Items.thorium};
        }};

        /*
        crags = new Zone("crags", new MapGenerator("crags").dist(2f)){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 2000, Items.lead, 2000, Items.graphite, 500, Items.titanium, 500, Items.silicon, 500);
            conditionWave = 3;
            launchPeriod = 2;
            zoneRequirements = ZoneRequirement.with(stainedMountains, 40);
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.sand};
        }};*/

        nuclearComplex = new Zone("nuclearComplex", new MapGenerator("nuclearProductionComplex", 1)
        .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.01))){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 1250, Items.lead, 1500, Items.silicon, 400, Items.metaglass, 250);
            conditionWave = 30;
            launchPeriod = 15;
            zoneRequirements = ZoneRequirement.with(fungalPass, 8);
            blockRequirements = new Block[]{Blocks.thermalGenerator, Blocks.laserDrill};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.thorium, Items.sand};
        }};

        /*
        impact0078 = new Zone("impact0078", new MapGenerator("impact0078").dist(2f)){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 2000, Items.lead, 2000, Items.graphite, 500, Items.titanium, 500, Items.silicon, 500);
            conditionWave = 3;
            launchPeriod = 2;
            zoneRequirements = ZoneRequirement.with(nuclearComplex, 40);
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.thorium};
        }};*/
    }
}
