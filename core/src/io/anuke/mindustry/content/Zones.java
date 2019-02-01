package io.anuke.mindustry.content;

import io.anuke.arc.collection.Array;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.maps.generators.MapGenerator;
import io.anuke.mindustry.maps.generators.MapGenerator.Decoration;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.world.Block;

public class Zones implements ContentList{
    public static Zone groundZero, craters, frozenForest, ruinousShores, crags, stainedMountains,
    impact, desolateRift, arcticDesert, dryWastes, nuclearComplex, moltenFault;

    @Override
    public void load(){

        groundZero = new Zone("groundZero", new MapGenerator("groundZero", 1)){{
            deployCost = ItemStack.with(Items.copper, 60);
            startingItems = ItemStack.with(Items.copper, 50);
            alwaysUnlocked = true;
            conditionWave = 10;
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 2; //2 mins
                spawns = Array.with(
                    new SpawnGroup(UnitTypes.dagger){{
                        unitScaling = 2;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 10;
                        unitScaling = 2;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 15;
                        unitScaling = 1;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 20;
                        unitScaling = 1;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 25;
                        unitScaling = 1;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 30;
                        unitScaling = 1;
                    }}
                );
            }};
        }};

        craters = new Zone("craters", new MapGenerator("craters", 1).dist(0)){{
            deployCost = ItemStack.with(Items.copper, 300);
            startingItems = ItemStack.with(Items.copper, 200);
            conditionWave = 15;
            itemRequirements = ItemStack.with(Items.copper, 2000);
            zoneRequirements = new Zone[]{groundZero};
            blockRequirements = new Block[]{Blocks.router};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60;
                spawns = Array.with(
                    new SpawnGroup(UnitTypes.dagger){{
                        unitScaling = 2;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        begin = 5;
                        unitAmount = 2;
                        spacing = 2;
                        unitScaling = 2;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 10;
                        unitScaling = 1;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        begin = 15;
                        unitScaling = 1;
                        unitAmount = 2;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 20;
                        unitScaling = 1;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        begin = 25;
                        unitScaling = 1;
                        unitAmount = 2;
                    }}
                );
            }};
        }};

        frozenForest = new Zone("frozenForest", new MapGenerator("frozenForest", 2)
            .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.02))){{
            alwaysUnlocked = true;
            deployCost = ItemStack.with(Items.copper, 500);
            startingItems = ItemStack.with(Items.copper, 400);
            conditionWave = 15;
            zoneRequirements = new Zone[]{craters};
            itemRequirements = ItemStack.with(Items.copper, 4000, Items.lead, 2000);
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60;
                spawns = Array.with(
                    new SpawnGroup(UnitTypes.titan){{
                        unitScaling = 3;
                        end = 9;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        unitScaling = 2;
                        begin = 2;
                        end = 9;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        unitScaling = 2;
                        begin = 5;
                        end = 9;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        unitScaling = 0.5f;
                        begin = 10;
                        spacing = 10;
                        unitAmount = 5;
                    }},

                    new SpawnGroup(UnitTypes.titan){{
                        begin = 11;
                        unitAmount = 2;
                        unitScaling = 2;
                        spacing = 2;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 12;
                        unitAmount = 2;
                        unitScaling = 2;
                        spacing = 2;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        unitScaling = 0.5f;
                        begin = 35;
                        spacing = 10;
                        unitAmount = 6;
                    }}
                );
            }};
        }};

        ruinousShores = new Zone("ruinousShores", new MapGenerator("ruinousShores", 1)){{
            deployCost = ItemStack.with(Items.copper, 600, Items.graphite, 50);
            startingItems = ItemStack.with(Items.copper, 400);
            conditionWave = 20;
            launchPeriod = 20;
            zoneRequirements = new Zone[]{frozenForest};
            itemRequirements = ItemStack.with(Items.lead, 6000, Items.graphite, 2000);
            blockRequirements = new Block[]{Blocks.graphitePress, Blocks.combustionGenerator};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60;
                spawns = Array.with(
                    new SpawnGroup(UnitTypes.wraith){{
                        unitScaling = 2;
                        spacing = 2;
                        end = 10;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 2;
                        spacing = 2;
                        unitScaling = 2;
                    }},

                    new SpawnGroup(UnitTypes.wraith){{
                        begin = 10;
                        unitScaling = 0.5f;
                        unitAmount = 6;
                        spacing = 10;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        begin = 5;
                        unitScaling = 1;
                        spacing = 5;
                        unitAmount = 1;
                        effect = StatusEffects.overdrive;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        begin = 22;
                        unitScaling = 1;
                        spacing = 20;
                        unitScaling = 0.5f;
                        unitAmount = 10;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 11;
                        spacing = 2;
                        unitScaling = 2;
                        unitAmount = 2;
                    }}
                );
            }};
        }};

        /*
        crags = new Zone("crags", new MapGenerator("groundZero", 1)){{ //TODO implement
            deployCost = ItemStack.with(Items.copper, 300);
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
            .decor(new Decoration(Blocks.stainedStone, Blocks.stainedBoulder, 0.01))){{

            deployCost = ItemStack.with(Items.copper, 500, Items.lead, 300, Items.silicon, 100);
            startingItems = ItemStack.with(Items.copper, 400, Items.lead, 100);
            conditionWave = 10;
            launchPeriod = 10;
            zoneRequirements = new Zone[]{frozenForest};
            blockRequirements = new Block[]{Blocks.pneumaticDrill};
            itemRequirements = ItemStack.with(Items.copper, 8000, Items.silicon, 2000);
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 2;
                spawns = Array.with(
                    new SpawnGroup(UnitTypes.titan){{
                        unitScaling = 2;
                        spacing = 2;
                        end = 10;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 2;
                        unitScaling = 1;
                        spacing = 2;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 10;
                        spacing = 2;
                        unitScaling = 2;
                        unitAmount = 2;
                    }},

                    new SpawnGroup(UnitTypes.ghoul){{
                        begin = 5;
                        unitScaling = 0.5f;
                        unitAmount = 1;
                        spacing = 5;
                    }},

                    new SpawnGroup(UnitTypes.wraith){{
                        begin = 10;
                        unitScaling = 1f;
                        unitAmount = 1;
                        spacing = 5;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 2;
                        unitScaling = 1;
                        spacing = 2;
                    }},

                    new SpawnGroup(UnitTypes.wraith){{
                        begin = 23;
                        unitScaling = 1f;
                        unitAmount = 1;
                        spacing = 2;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        begin = 20;
                        unitScaling = 1;
                        spacing = 10;
                        unitScaling = 0.5f;
                        unitAmount = 10;
                    }}
                );
            }};
        }};

        impact = new Zone("impact0079", new MapGenerator("impact0079")){{ //TODO implement
            deployCost = ItemStack.with(Items.copper, 4000);
            startingItems = ItemStack.with(Items.copper, 200);
            conditionWave = 15;
            zoneRequirements = new Zone[]{stainedMountains};
            blockRequirements = new Block[]{Blocks.launchPad};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 80;
            }};
        }};

        /*

        desolateRift = new Zone("desolateRift", new MapGenerator("groundZero", 1)){{ //TODO implement
            deployCost = ItemStack.with(Items.copper, 300);
            startingItems = ItemStack.with(Items.copper, 200);
            conditionWave = 15;
            zoneRequirements = new Zone[]{frozenForest};
            blockRequirements = new Block[]{Blocks.copperWall};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 80;
            }};
        }};

        arcticDesert = new Zone("arcticDesert", new MapGenerator("groundZero", 1)){{ //TODO implement
            deployCost = ItemStack.with(Items.copper, 300);
            startingItems = ItemStack.with(Items.copper, 200);
            conditionWave = 15;
            zoneRequirements = new Zone[]{frozenForest};
            blockRequirements = new Block[]{Blocks.copperWall};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 80;
            }};
        }};

        dryWastes = new Zone("dryWastes", new MapGenerator("groundZero", 1)){{ //TODO implement
            deployCost = ItemStack.with(Items.copper, 300);
            startingItems = ItemStack.with(Items.copper, 200);
            conditionWave = 15;
            zoneRequirements = new Zone[]{frozenForest};
            blockRequirements = new Block[]{Blocks.copperWall};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 80;
            }};
        }};

        nuclearComplex = new Zone("nuclearComplex", new MapGenerator("groundZero", 1)){{ //TODO implement
            deployCost = ItemStack.with(Items.copper, 300);
            startingItems = ItemStack.with(Items.copper, 200);
            conditionWave = 15;
            zoneRequirements = new Zone[]{frozenForest};
            blockRequirements = new Block[]{Blocks.copperWall};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 80;
            }};
        }};

        moltenFault = new Zone("moltenFault", new MapGenerator("groundZero", 1)){{ //TODO implement
            deployCost = ItemStack.with(Items.copper, 300);
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
    }
}
