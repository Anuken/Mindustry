package io.anuke.mindustry.content;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.maps.generators.MapGenerator;
import io.anuke.mindustry.maps.generators.MapGenerator.Decoration;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.world.Block;

public class Zones implements ContentList{
    public static Zone groundZero, craters, frozenForest, ruinousShores, stainedMountains,
    desolateRift, nuclearComplex;

    @Override
    public void load(){

        groundZero = new Zone("groundZero", new MapGenerator("groundZero", 1)){{
            baseLaunchCost = ItemStack.with(Items.copper, -100);
            startingItems = ItemStack.list(Items.copper, 100);
            alwaysUnlocked = true;
            conditionWave = 5;
            launchPeriod = 5;
            resources = new Item[]{Items.copper, Items.scrap, Items.lead};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 2;
            }};
        }};

        craters = new Zone("craters", new MapGenerator("craters", 1).dist(0)){{
            startingItems = ItemStack.list(Items.copper, 200);
            conditionWave = 10;
            itemRequirements = ItemStack.with(Items.copper, 2000);
            zoneRequirements = new Zone[]{groundZero};
            blockRequirements = new Block[]{Blocks.router};
            resources = new Item[]{Items.copper, Items.lead};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 1f;
            }};
        }};

        frozenForest = new Zone("frozenForest", new MapGenerator("frozenForest", 1)
            .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.02))){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 400);
            conditionWave = 10;
            zoneRequirements = new Zone[]{craters};
            itemRequirements = ItemStack.with(Items.copper, 4000, Items.lead, 2000);
            resources = new Item[]{Items.copper, Items.lead, Items.coal};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 1.5f;
            }};
        }};

        ruinousShores = new Zone("ruinousShores", new MapGenerator("ruinousShores", 1).dist(3f, true)){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 400);
            conditionWave = 20;
            launchPeriod = 20;
            zoneRequirements = new Zone[]{frozenForest};
            itemRequirements = ItemStack.with(Items.lead, 6000, Items.graphite, 2000);
            blockRequirements = new Block[]{Blocks.graphitePress, Blocks.combustionGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.sand};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 80;
            }};
        }};

        /*
        crags = new Zone("crags", new MapGenerator("groundZero", 1)){{ //TODO implement
            baseLaunchCost = ItemStack.with(Items.copper, 300);
            startingItems = ItemStack.with(Items.copper, 200);
            conditionWave = 15;
            zoneRequirements = new Zone[]{frozenForest};
            blockRequirements = new Block[]{Blocks.copperWall};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 80;
            }};
        }};*/

        stainedMountains = new Zone("stainedMountains", new MapGenerator("stainedMountains", 2)
            .dist(2.5f, true)
            .decor(new Decoration(Blocks.moss, Blocks.shaleBoulder, 0.02))){{
            loadout = Loadouts.basicFoundation;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 400, Items.lead, 100);
            conditionWave = 10;
            launchPeriod = 10;
            zoneRequirements = new Zone[]{frozenForest};
            blockRequirements = new Block[]{Blocks.pneumaticDrill};
            itemRequirements = ItemStack.with(Items.copper, 8000, Items.silicon, 2000);
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 2;
            }};
        }};

        desolateRift = new Zone("desolateRift", new MapGenerator("desolateRift").dist(2f)){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 1500);
            itemRequirements = ItemStack.with(Items.copper, 8000, Items.metaglass, 2000, Items.graphite, 3000);
            conditionWave = 10;
            launchPeriod = 20;
            zoneRequirements = new Zone[]{ruinousShores};
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.sand};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 1.5f;
            }};
        }};

        nuclearComplex = new Zone("nuclearComplex", new MapGenerator("nuclearProductionComplex", 1)
        .drops(ItemStack.with(Items.copper, 2000, Items.lead, 1500, Items.silicon, 1000, Items.graphite, 1000, Items.thorium, 200, Items.titanium, 2000, Items.metaglass, 1000))
        .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.01))){{
            loadout = Loadouts.basicNucleus;
            baseLaunchCost = ItemStack.with();
            startingItems = ItemStack.list(Items.copper, 2500, Items.lead, 3000, Items.silicon, 800, Items.metaglass, 400);
            itemRequirements = ItemStack.with(Items.copper, 10000, Items.titanium, 8000, Items.metaglass, 6000, Items.plastanium, 2000);
            conditionWave = 30;
            launchPeriod = 15;
            zoneRequirements = new Zone[]{desolateRift};
            blockRequirements = new Block[]{Blocks.blastDrill, Blocks.thermalGenerator};
            resources = new Item[]{Items.copper, Items.scrap, Items.lead, Items.coal, Items.titanium, Items.thorium, Items.sand};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 2;
            }};
        }};
    }
}
