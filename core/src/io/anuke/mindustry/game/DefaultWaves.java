package io.anuke.mindustry.game;

import io.anuke.arc.collection.Array;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.type.ItemStack;

public class DefaultWaves{
    private Array<SpawnGroup> spawns;

    public Array<SpawnGroup> get(){
        if(spawns == null && UnitTypes.dagger != null){
            spawns = Array.with(
            new SpawnGroup(UnitTypes.dagger){{
                end = 10;
                unitScaling = 2f;
            }},

            new SpawnGroup(UnitTypes.crawler){{
                begin = 4;
                end = 13;
                unitAmount = 2;
                unitScaling = 1.5f;
            }},

            new SpawnGroup(UnitTypes.wraith){{
                begin = 12;
                end = 16;
                unitScaling = 1f;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 11;
                unitScaling = 1.7f;
                spacing = 2;
                max = 4;
            }},

            new SpawnGroup(UnitTypes.titan){{
                begin = 7;
                spacing = 3;
                unitScaling = 2;

                end = 30;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 8;
                unitScaling = 1;
                unitAmount = 4;
                spacing = 2;
            }},

            new SpawnGroup(UnitTypes.titan){{
                begin = 28;
                spacing = 3;
                unitScaling = 1;
                end = 40;
            }},

            new SpawnGroup(UnitTypes.titan){{
                begin = 45;
                spacing = 3;
                unitScaling = 2;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.titan){{
                begin = 120;
                spacing = 2;
                unitScaling = 3;
                unitAmount = 5;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.wraith){{
                begin = 16;
                unitScaling = 1;
                spacing = 2;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 82;
                spacing = 3;
                unitAmount = 4;
                unitScaling = 3;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 41;
                spacing = 5;
                unitAmount = 1;
                unitScaling = 3;
                effect = StatusEffects.shielded;
            }},

            new SpawnGroup(UnitTypes.fortress){{
                begin = 40;
                spacing = 5;
                unitAmount = 2;
                unitScaling = 2;
                max = 20;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 35;
                spacing = 3;
                unitAmount = 4;
                effect = StatusEffects.overdrive;
                items = new ItemStack(Items.blastCompound, 60);
                end = 60;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 42;
                spacing = 3;
                unitAmount = 4;
                effect = StatusEffects.overdrive;
                items = new ItemStack(Items.pyratite, 100);
                end = 130;
            }},

            new SpawnGroup(UnitTypes.ghoul){{
                begin = 40;
                unitAmount = 2;
                spacing = 2;
                unitScaling = 2;
            }},

            new SpawnGroup(UnitTypes.wraith){{
                begin = 50;
                unitAmount = 4;
                unitScaling = 3;
                spacing = 5;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.revenant){{
                begin = 50;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 5;
                max = 16;
            }},

            new SpawnGroup(UnitTypes.ghoul){{
                begin = 53;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
            }},

            new SpawnGroup(UnitTypes.eruptor){{
                begin = 31;
                unitAmount = 4;
                unitScaling = 1;
                spacing = 3;
            }},

            new SpawnGroup(UnitTypes.chaosArray){{
                begin = 41;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 30;
            }},

            new SpawnGroup(UnitTypes.eradicator){{
                begin = 81;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 40;
            }},

            new SpawnGroup(UnitTypes.lich){{
                begin = 131;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 40;
            }},

            new SpawnGroup(UnitTypes.ghoul){{
                begin = 90;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
            }}
            );
        }
        return spawns == null ? new Array<>() : spawns;
    }
}
