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
    craters, frozenForest, ruinousShores, stainedMountains, tarFields,
    saltFlats, overgrowth, infestedIslands,
    desolateRift, nuclearComplex;

    @Override
    public void load(){

        groundZero = new Zone("groundZero", new MapGenerator("groundZero", 1).decor(new Decoration(Blocks.snow, Blocks.snowrock, 0.01))){{
            baseLaunchCost = ItemStack.with(Items.copper, -100);
            startingItems = ItemStack.list(Items.copper, 100);
            alwaysUnlocked = true;
            conditionWave = 5;
            launchPeriod = 5;
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.sand};
        }};

        desertWastes = new Zone("desertWastes", new DesertWastesGenerator(260, 260)){{
            startingItems = ItemStack.list(Items.copper, 200);
            conditionWave = 20;
            launchPeriod = 10;
            loadout = Loadouts.advancedShard;
            zoneRequirements = ZoneRequirement.with(groundZero, 20);
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
            baseLaunchCost = ItemStack.with(Items.copper, -100);
            startingItems = ItemStack.list(Items.copper, 100);
            alwaysUnlocked = true;
            conditionWave = 5;
            launchPeriod = 5;
            loadout = Loadouts.basicFoundation;
            zoneRequirements = ZoneRequirement.with(desertWastes, 60);
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.sand};
        }};

        craters = new Zone("craters", new MapGenerator("craters", 1).dist(0).decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.01))){{
            startingItems = ItemStack.list(Items.copper, 200);
            conditionWave = 10;
            zoneRequirements = ZoneRequirement.with(groundZero, 10);
            blockRequirements = new Block[]{Blocks.router};
            resources = new Item[]{Items.copper, Items.lead, Items.coal};
        }};

        frozenForest = new Zone("frozenForest", new MapGenerator("frozenForest", 1)
        .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.02))){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 400);
            conditionWave = 10;
            zoneRequirements = ZoneRequirement.with(craters, 10);
            resources = new Item[]{Items.copper, Items.lead, Items.coal};
        }};

        overgrowth = new Zone("overgrowth", new MapGenerator("overgrowth")){{
            startingItems = ItemStack.list(Items.copper, 3000, Items.lead, 2000, Items.silicon, 1000, Items.metaglass, 500);
            conditionWave = 12;
            launchPeriod = 4;
            loadout = Loadouts.basicNucleus;
            zoneRequirements = ZoneRequirement.with(frozenForest, 40);
            blockRequirements = new Block[]{Blocks.router};
            resources = new Item[]{Items.copper, Items.lead, Items.coal, Items.titanium, Items.sand, Items.thorium, Items.scrap};
        }};

        ruinousShores = new Zone("ruinousShores", new MapGenerator("ruinousShores", 1).dist(3f, true)){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 400);
            conditionWave = 20;
            launchPeriod = 20;
            zoneRequirements = ZoneRequirement.with(desertWastes, 20, craters, 15);
            blockRequirements = new Block[]{Blocks.graphitePress, Blocks.combustionGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.sand};
        }};

        stainedMountains = new Zone("stainedMountains", new MapGenerator("stainedMountains", 2)
        .dist(0f, false)
        .decor(new Decoration(Blocks.shale, Blocks.shaleBoulder, 0.02))){{
            loadout = Loadouts.basicFoundation;
            startingItems = ItemStack.list(Items.copper, 400, Items.lead, 100);
            conditionWave = 10;
            launchPeriod = 10;
            zoneRequirements = ZoneRequirement.with(frozenForest, 15);
            blockRequirements = new Block[]{Blocks.pneumaticDrill};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.sand};
        }};

        tarFields = new Zone("tarFields", new MapGenerator("tarFields")
        .dist(0f, false)
        .decor(new Decoration(Blocks.shale, Blocks.shaleBoulder, 0.02))){{
            loadout = Loadouts.basicFoundation;
            startingItems = ItemStack.list(Items.copper, 500, Items.lead, 200);
            conditionWave = 15;
            launchPeriod = 10;
            zoneRequirements = ZoneRequirement.with(ruinousShores, 20);
            blockRequirements = new Block[]{Blocks.coalCentrifuge};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.sand};
        }};

        desolateRift = new Zone("desolateRift", new MapGenerator("desolateRift").dist(2f)){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 2000, Items.lead, 2000, Items.graphite, 500, Items.titanium, 500, Items.silicon, 500);
            conditionWave = 3;
            launchPeriod = 2;
            zoneRequirements = ZoneRequirement.with(tarFields, 20);
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.sand, Items.thorium};
        }};

        nuclearComplex = new Zone("nuclearComplex", new MapGenerator("nuclearProductionComplex", 1)
        .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.01))){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 2500, Items.lead, 3000, Items.silicon, 800, Items.metaglass, 400);
            conditionWave = 30;
            launchPeriod = 15;
            zoneRequirements = ZoneRequirement.with(stainedMountains, 20);
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.thorium, Items.sand};
        }};
    }
}
