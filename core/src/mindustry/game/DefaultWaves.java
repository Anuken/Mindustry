package mindustry.game;

import arc.struct.Seq;
import mindustry.content.*;
import mindustry.type.ItemStack;

public class DefaultWaves{
    private Seq<SpawnGroup> spawns;

    public Seq<SpawnGroup> get(){
        if(spawns == null && UnitTypes.dagger != null){
            spawns = Seq.with(
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

            new SpawnGroup(UnitTypes.flare){{
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

            new SpawnGroup(UnitTypes.pulsar){{
                begin = 13;
                spacing = 3;
                unitScaling = 0.5f;
            }},

            new SpawnGroup(UnitTypes.mace){{
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

            new SpawnGroup(UnitTypes.mace){{
                begin = 28;
                spacing = 3;
                unitScaling = 1;
                end = 40;
            }},

            new SpawnGroup(UnitTypes.mace){{
                begin = 45;
                spacing = 3;
                unitScaling = 2;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.mace){{
                begin = 120;
                spacing = 2;
                unitScaling = 3;
                unitAmount = 5;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.flare){{
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

            new SpawnGroup(UnitTypes.horizon){{
                begin = 40;
                unitAmount = 2;
                spacing = 2;
                unitScaling = 2;
            }},

            new SpawnGroup(UnitTypes.flare){{
                begin = 50;
                unitAmount = 4;
                unitScaling = 3;
                spacing = 5;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(UnitTypes.zenith){{
                begin = 50;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 5;
                max = 16;
            }},

            new SpawnGroup(UnitTypes.horizon){{
                begin = 53;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
            }},

            new SpawnGroup(UnitTypes.atrax){{
                begin = 31;
                unitAmount = 4;
                unitScaling = 1;
                spacing = 3;
            }},

            new SpawnGroup(UnitTypes.scepter){{
                begin = 41;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 30;
            }},

            new SpawnGroup(UnitTypes.reign){{
                begin = 81;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 40;
            }},

            new SpawnGroup(UnitTypes.antumbra){{
                begin = 131;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 40;
            }},

            new SpawnGroup(UnitTypes.horizon){{
                begin = 90;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
            }}
            );
        }
        return spawns == null ? new Seq<>() : spawns;
    }
}
