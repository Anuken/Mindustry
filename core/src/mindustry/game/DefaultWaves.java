package mindustry.game;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.type.*;

import static mindustry.content.UnitTypes.*;

public class DefaultWaves{
    private Seq<SpawnGroup> spawns;

    public Seq<SpawnGroup> get(){
        if(spawns == null && dagger != null){
            spawns = Seq.with(
            new SpawnGroup(dagger){{
                end = 10;
                unitScaling = 2f;
            }},

            new SpawnGroup(crawler){{
                begin = 4;
                end = 13;
                unitAmount = 2;
                unitScaling = 1.5f;
            }},

            new SpawnGroup(flare){{
                begin = 12;
                end = 16;
                unitScaling = 1f;
            }},

            new SpawnGroup(dagger){{
                begin = 11;
                unitScaling = 1.7f;
                spacing = 2;
                max = 4;
            }},

            new SpawnGroup(pulsar){{
                begin = 13;
                spacing = 3;
                unitScaling = 0.5f;
            }},

            new SpawnGroup(mace){{
                begin = 7;
                spacing = 3;
                unitScaling = 2;

                end = 30;
            }},

            new SpawnGroup(dagger){{
                begin = 8;
                unitScaling = 1;
                unitAmount = 4;
                spacing = 2;
            }},

            new SpawnGroup(mace){{
                begin = 28;
                spacing = 3;
                unitScaling = 1;
                end = 40;
            }},

            new SpawnGroup(mace){{
                begin = 45;
                spacing = 3;
                unitScaling = 2;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(mace){{
                begin = 120;
                spacing = 2;
                unitScaling = 3;
                unitAmount = 5;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(flare){{
                begin = 16;
                unitScaling = 1;
                spacing = 2;
            }},

            new SpawnGroup(dagger){{
                begin = 82;
                spacing = 3;
                unitAmount = 4;
                unitScaling = 3;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(dagger){{
                begin = 41;
                spacing = 5;
                unitAmount = 1;
                unitScaling = 3;
                effect = StatusEffects.shielded;
            }},

            new SpawnGroup(fortress){{
                begin = 40;
                spacing = 5;
                unitAmount = 2;
                unitScaling = 2;
                max = 20;
            }},

            new SpawnGroup(dagger){{
                begin = 35;
                spacing = 3;
                unitAmount = 4;
                effect = StatusEffects.overdrive;
                items = new ItemStack(Items.blastCompound, 60);
                end = 60;
            }},

            new SpawnGroup(dagger){{
                begin = 42;
                spacing = 3;
                unitAmount = 4;
                effect = StatusEffects.overdrive;
                items = new ItemStack(Items.pyratite, 100);
                end = 130;
            }},

            new SpawnGroup(horizon){{
                begin = 40;
                unitAmount = 2;
                spacing = 2;
                unitScaling = 2;
            }},

            new SpawnGroup(flare){{
                begin = 50;
                unitAmount = 4;
                unitScaling = 3;
                spacing = 5;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(zenith){{
                begin = 50;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 5;
                max = 16;
            }},

            new SpawnGroup(horizon){{
                begin = 53;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
            }},

            new SpawnGroup(atrax){{
                begin = 31;
                unitAmount = 4;
                unitScaling = 1;
                spacing = 3;
            }},

            new SpawnGroup(scepter){{
                begin = 41;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 30;
            }},

            new SpawnGroup(reign){{
                begin = 81;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 40;
            }},

            new SpawnGroup(antumbra){{
                begin = 131;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 40;
            }},

            new SpawnGroup(horizon){{
                begin = 90;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
            }}
            );
        }
        return spawns == null ? new Seq<>() : spawns;
    }

    //TODO move elsewhere
    public static Seq<SpawnGroup> generate(){
        UnitType[][] species = {
        {dagger, mace, fortress, scepter, reign},
        {nova, pulsar, quasar, vela, corvus},
        {crawler, atrax, spiroct, arkyid, toxopid},
        //{risso, minke, bryde, sei, omura}, //questionable choices
        //{mono, poly, mega, quad, oct}, //do not attack
        {flare, horizon, zenith, antumbra, eclipse}
        };

        //required progression:
        //- extra periodic patterns

        Seq<SpawnGroup> out = new Seq<>();

        //max reasonable wave, after which everything gets boring
        int cap = 400;

        //main sequence
        float shieldStart = 30, shieldsPerWave = 12;
        UnitType[] curSpecies = Structs.random(species);
        int curTier = 0;

        for(int i = 0; i < cap;){
            int f = i;
            int next = Mathf.random(15, 25);

            float shieldAmount = Math.max((i - shieldStart) * shieldsPerWave, 0);

            //main progression
            out.add(new SpawnGroup(curSpecies[Math.min(curTier, curSpecies.length - 1)]){{
                unitAmount = f == 0 ? 1 : 10;
                begin = f;
                end = f + next >= cap ? never : f + next;
                max = 16;
                unitScaling = Mathf.random(1f, 2f);
                shields = shieldAmount;
                shieldScaling = shieldsPerWave;
            }});

            //extra progression that tails out, blends in
            out.add(new SpawnGroup(curSpecies[Math.min(curTier, curSpecies.length - 1)]){{
                unitAmount = 6;
                begin = f + next;
                end = f + next + Mathf.random(8, 12);
                max = 10;
                unitScaling = Mathf.random(2f);
                spacing = Mathf.random(2, 3);
                shields = shieldAmount;
                shieldScaling = shieldsPerWave;
            }});

            i += next;
            if(curTier < 3 || Mathf.chance(0.2)){
                curTier ++;
            }

            //do not spawn bosses
            curTier = Math.min(curTier, 3);

            //small chance to switch species
            if(Mathf.chance(0.3)){
                curSpecies = Structs.random(species);
            }
        }

        int bossWave = Mathf.random(30, 60);
        int bossSpacing = Mathf.random(30, 50);

        //main boss progression
        out.add(new SpawnGroup(Structs.random(species)[4]){{
            unitAmount = 1;
            begin = bossWave;
            spacing = bossSpacing;
            end = never;
            max = 16;
            unitScaling = bossSpacing;
            shieldScaling = shieldsPerWave;
        }});

        //alt boss progression
        out.add(new SpawnGroup(Structs.random(species)[4]){{
            unitAmount = 1;
            begin = bossWave + Mathf.random(4, 6) * bossSpacing;
            spacing = bossSpacing;
            end = never;
            max = 16;
            unitScaling = bossSpacing;
            shieldScaling = shieldsPerWave;
        }});

        return out;
    }
}
