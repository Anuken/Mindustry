package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.content.Weapons;
import io.anuke.mindustry.type.ItemStack;

public class Waves{

    public static Array<SpawnGroup> getSpawns(){
        return Array.with(
            new SpawnGroup(UnitTypes.dagger){{
                end = 8;
                unitScaling = 3;
            }},

            new SpawnGroup(UnitTypes.wraith){{
                begin = 12;
                end = 14;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 11;
                unitScaling = 2;
                spacing = 2;
                max = 4;
            }},

            new SpawnGroup(UnitTypes.titan){{
                begin = 9;
                spacing = 3;
                unitScaling = 2;

                end = 30;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 10;
                unitScaling = 2;
                unitAmount = 1;
                spacing = 2;
                end = 30;
            }},

            new SpawnGroup(UnitTypes.titan){{
                begin = 28;
                spacing = 3;
                unitScaling = 2;
                weapon = Weapons.flamethrower;
                end = 40;
            }},

            new SpawnGroup(UnitTypes.titan){{
                begin = 45;
                spacing = 3;
                unitScaling = 2;
                weapon = Weapons.flamethrower;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.titan){{
                begin = 120;
                spacing = 2;
                unitScaling = 3;
                unitAmount = 5;
                weapon = Weapons.flakgun;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.wraith){{
                begin = 16;
                unitScaling = 2;
                spacing = 2;

                end = 39;
                max = 7;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 82;
                spacing = 3;
                unitAmount = 4;
                groupAmount = 2;
                unitScaling = 3;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 41;
                spacing = 5;
                unitAmount = 1;
                unitScaling = 3;
                effect = StatusEffects.shielded;
                max = 10;
            }},

            new SpawnGroup(UnitTypes.fortress){{
                begin = 40;
                spacing = 5;
                unitAmount = 2;
                unitScaling = 3;
                max = 10;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 35;
                spacing = 3;
                unitAmount = 4;
                groupAmount = 2;
                effect = StatusEffects.overdrive;
                items = new ItemStack(Items.blastCompound, 60);
                end = 60;
            }},

            new SpawnGroup(UnitTypes.dagger){{
                begin = 42;
                spacing = 3;
                unitAmount = 4;
                groupAmount = 2;
                effect = StatusEffects.overdrive;
                items = new ItemStack(Items.pyratite, 100);
                end = 130;
            }},

            new SpawnGroup(UnitTypes.ghoul){{
                begin = 40;
                unitAmount = 2;
                spacing = 2;
                unitScaling = 3;
                max = 8;
            }},

            new SpawnGroup(UnitTypes.wraith){{
                begin = 50;
                unitAmount = 4;
                unitScaling = 3;
                spacing = 5;
                groupAmount = 2;
                effect = StatusEffects.overdrive;
                max = 8;
            }},

            new SpawnGroup(UnitTypes.revenant){{
                begin = 50;
                unitAmount = 4;
                unitScaling = 3;
                spacing = 5;
                groupAmount = 2;
                max = 8;
            }},

            new SpawnGroup(UnitTypes.ghoul){{
                begin = 53;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
                max = 8;
                end = 74;
            }},

            new SpawnGroup(UnitTypes.ghoul){{
                begin = 53;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
                max = 8;
                end = 74;
            }}
        );
    }

    public static void testWaves(int from, int to){
        Array<SpawnGroup> spawns = getSpawns();
        for(int i = from; i <= to; i++){
            System.out.print(i + ": ");
            int total = 0;
            for(SpawnGroup spawn : spawns){
                int a = spawn.getUnitsSpawned(i) * spawn.getGroupsSpawned(i);
                total += a;

                if(a > 0){
                    System.out.print(a + "x" + spawn.type.name);

                    if(spawn.weapon != null){
                        System.out.print(":" + spawn.weapon.name);
                    }

                    System.out.print(" ");
                }
            }
            System.out.print(" (" + total + ")");
            System.out.println();
        }
    }
}
