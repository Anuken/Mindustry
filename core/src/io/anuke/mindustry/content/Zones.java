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
            launchPeriod = 5;
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 2; //2 mins
                spawns = Array.with(
                    new SpawnGroup(UnitTypes.dagger){{
                        unitScaling = 1.5f;
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
            deployCost = ItemStack.with(Items.copper, 200);
            startingItems = ItemStack.with(Items.copper, 200);
            conditionWave = 10;
            itemRequirements = ItemStack.with(Items.copper, 2000);
            zoneRequirements = new Zone[]{groundZero};
            blockRequirements = new Block[]{Blocks.router};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 1.5f;
                spawns = Array.with(
                    new SpawnGroup(UnitTypes.dagger){{
                        unitScaling = 1.5f;
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
            .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.02))
            .core(Blocks.coreFoundation)){{
            deployCost = ItemStack.with(Items.copper, 500);
            startingItems = ItemStack.with(Items.copper, 400);
            conditionWave = 10;
            zoneRequirements = new Zone[]{craters};
            itemRequirements = ItemStack.with(Items.copper, 4000, Items.lead, 2000);
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60 * 1.5f;
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

        ruinousShores = new Zone("ruinousShores", new MapGenerator("ruinousShores", 1)
            .core(Blocks.coreFoundation)){{
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
            .decor(new Decoration(Blocks.stainedStone, Blocks.stainedBoulder, 0.01))
            .core(Blocks.coreFoundation)){{
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

        impact = new Zone("impact0079", new MapGenerator("impact0079", 2)
        .decor(
            new Decoration(Blocks.snow, Blocks.sporeCluster, 0.01),
            new Decoration(Blocks.metalFloor, Blocks.metalFloorDamaged, 0.02)
        ).drops(ItemStack.with(Items.copper, 2000, Items.lead, 1500, Items.silicon, 1000, Items.graphite, 1000, Items.pyratite, 2000, Items.titanium, 2000, Items.metaglass, 1000))){{
            deployCost = ItemStack.with(Items.copper, 2500, Items.lead, 1000, Items.silicon, 300);
            startingItems = ItemStack.with(Items.copper, 2000, Items.lead, 500, Items.silicon, 200);
            itemRequirements = ItemStack.with(Items.silicon, 8000, Items.titanium, 6000, Items.graphite, 4000);
            conditionWave = 20;
            zoneRequirements = new Zone[]{stainedMountains};
            blockRequirements = new Block[]{Blocks.launchPad, Blocks.unloader, Blocks.coreFoundation};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 60 * 60;

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

        desolateRift = new Zone("desolateRift", new MapGenerator("desolateRift")
                .core(Blocks.coreFoundation).dist(2f)){{
            deployCost = ItemStack.with(Items.copper, 2000);
            startingItems = ItemStack.with(Items.copper, 1500);
            itemRequirements = ItemStack.with(Items.copper, 8000, Items.metaglass, 2000, Items.graphite, 3000);
            conditionWave = 10;
            launchPeriod = 20;
            zoneRequirements = new Zone[]{ruinousShores};
            blockRequirements = new Block[]{Blocks.thermalGenerator};
            rules = () -> new Rules(){{
                waves = true;
                waveTimer = true;
                waveSpacing = 30 * 60;
                spawns = Array.with(
                    new SpawnGroup(UnitTypes.crawler){{
                        unitScaling = 1;
                        spacing = 2;
                        end = 10;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 2;
                        spacing = 2;
                        unitScaling = 2;
                    }},

                    new SpawnGroup(UnitTypes.titan){{
                        begin = 10;
                        unitScaling = 1f;
                        unitAmount = 3;
                        spacing = 3;
                    }},

                    new SpawnGroup(UnitTypes.fortress){{
                        begin = 5;
                        unitScaling = 1;
                        spacing = 5;
                        unitAmount = 1;
                    }},

                    new SpawnGroup(UnitTypes.fortress){{
                        begin = 13;
                        unitScaling = 1;
                        spacing = 4;
                        unitAmount = 1;
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 11;
                        spacing = 2;
                        unitScaling = 2;
                        unitAmount = 2;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        unitScaling = 1;
                        spacing = 2;
                        unitAmount = 4;
                        begin = 13;
                    }}
                );
            }};
        }};

        /*
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

        */
        nuclearComplex = new Zone("nuclearComplex", new MapGenerator("nuclearProductionComplex", 1)
        .drops(ItemStack.with(Items.copper, 2000, Items.lead, 1500, Items.silicon, 1000, Items.graphite, 1000, Items.thorium, 200, Items.titanium, 2000, Items.metaglass, 1000))
        .decor(new Decoration(Blocks.snow, Blocks.sporeCluster, 0.01))){{
            deployCost = ItemStack.with(Items.copper, 3000, Items.lead, 2000, Items.silicon, 1000, Items.metaglass, 500);
            startingItems = ItemStack.with(Items.copper, 2500, Items.lead, 1500, Items.silicon, 800, Items.metaglass, 400);
            itemRequirements = ItemStack.with(Items.copper, 10000, Items.titanium, 8000, Items.metaglass, 6000, Items.plastanium, 2000);
            conditionWave = 30;
            launchPeriod = 15;
            zoneRequirements = new Zone[]{impact};
            blockRequirements = new Block[]{Blocks.blastDrill, Blocks.thermalGenerator};
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

        /*
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
