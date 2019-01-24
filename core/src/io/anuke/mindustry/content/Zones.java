package io.anuke.mindustry.content;

import io.anuke.arc.collection.Array;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.maps.generators.MapGenerator;
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

        craters = new Zone("craters", new MapGenerator("craters", 1){{ distortion = 1.44f; }}){{ //TODO implement
            alwaysUnlocked = true;

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
                    }},

                    new SpawnGroup(UnitTypes.dagger){{
                        begin = 20;
                        unitScaling = 1;
                    }},

                    new SpawnGroup(UnitTypes.crawler){{
                        begin = 25;
                        unitScaling = 1;
                    }}
                );
            }};
        }};

        frozenForest = new Zone("frozenForest", new MapGenerator("groundZero", 1)){{ //TODO implement
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

        ruinousShores = new Zone("ruinousShores", new MapGenerator("groundZero", 1)){{ //TODO implement
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
        }};

        stainedMountains = new Zone("stainedMountains", new MapGenerator("groundZero", 1)){{ //TODO implement
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

        impact = new Zone("impact", new MapGenerator("groundZero", 1)){{ //TODO implement
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
        }};

        frozenForest.zoneRequirements = new Zone[]{frozenForest};
    }
}
