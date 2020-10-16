package mindustry.game;

import arc.func.*;
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
                shieldScaling = 15f;
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
                begin = 12;
                unitScaling = 1;
                unitAmount = 4;
                spacing = 2;
                shieldScaling = 10f;
                max = 20;
            }},

            new SpawnGroup(mace){{
                begin = 28;
                spacing = 3;
                unitScaling = 1;
                end = 40;
                shieldScaling = 20f;
            }},

            new SpawnGroup(spiroct){{
                begin = 45;
                spacing = 3;
                unitScaling = 1;
                max = 10;
                shieldScaling = 10f;
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
                shieldScaling = 20f;
            }},

            new SpawnGroup(quasar){{
                begin = 82;
                spacing = 3;
                unitAmount = 4;
                unitScaling = 3;
                shieldScaling = 30f;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(pulsar){{
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
                shieldScaling = 30;
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
                shieldScaling = 20;
            }},

            new SpawnGroup(flare){{
                begin = 50;
                unitAmount = 4;
                unitScaling = 3;
                spacing = 5;
                shields = 100f;
                shieldScaling = 10f;
                effect = StatusEffects.overdrive;
            }},

            new SpawnGroup(zenith){{
                begin = 50;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 5;
                max = 16;
                shieldScaling = 30;
            }},

            new SpawnGroup(nova){{
                begin = 53;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
                shieldScaling = 20;
            }},

            new SpawnGroup(atrax){{
                begin = 31;
                unitAmount = 4;
                unitScaling = 1;
                spacing = 3;
                shieldScaling = 5f;
            }},

            new SpawnGroup(scepter){{
                begin = 41;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 30;
                shieldScaling = 10f;
            }},

            new SpawnGroup(reign){{
                begin = 81;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 40;
                shieldScaling = 20f;
            }},

            new SpawnGroup(antumbra){{
                begin = 120;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 40;
                shieldScaling = 20f;
            }},

            new SpawnGroup(vela){{
                begin = 100;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 30;
                shieldScaling = 20f;
            }},

            new SpawnGroup(corvus){{
                begin = 145;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 35;
                shieldScaling = 30f;
                shields = 100;
            }},

            new SpawnGroup(horizon){{
                begin = 90;
                unitAmount = 2;
                unitScaling = 3;
                spacing = 4;
                shields = 40f;
                shieldScaling = 20f;
            }},

            new SpawnGroup(atrax){{
                begin = 210;
                unitAmount = 1;
                unitScaling = 1;
                spacing = 35;
                shields = 1000;
                shieldScaling = 35f;
            }}
            );
        }
        return spawns == null ? new Seq<>() : spawns;
    }

    //TODO move elsewhere
    public static Seq<SpawnGroup> generate(float difficulty){
        return generate(new Rand(), difficulty);
    }

    //TODO move elsewhere
    public static Seq<SpawnGroup> generate(Rand rand, float difficulty){
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
        int cap = 150;

        float shieldStart = 30, shieldsPerWave = 20 + difficulty*30f;

        Intc createProgression = start -> {
            //main sequence
            UnitType[] curSpecies = Structs.random(species);
            int curTier = 0;

            for(int i = start; i < cap;){
                int f = i;
                int next = rand.random(8, 16);

                float shieldAmount = Math.max((i - shieldStart) * shieldsPerWave, 0);
                int space = start == 0 ? 1 : rand.random(1, 2);

                //main progression
                out.add(new SpawnGroup(curSpecies[Math.min(curTier, curSpecies.length - 1)]){{
                    unitAmount = f == 0 ? 1 : 10;
                    begin = f;
                    end = f + next >= cap ? never : f + next;
                    max = 14;
                    unitScaling = rand.random(1f, 3f);
                    shields = shieldAmount;
                    shieldScaling = shieldsPerWave;
                    spacing = space;
                }});

                //extra progression that tails out, blends in
                out.add(new SpawnGroup(curSpecies[Math.min(curTier, curSpecies.length - 1)]){{
                    unitAmount = 6;
                    begin = f + next;
                    end = f + next + rand.random(8, 12);
                    max = 11;
                    unitScaling = rand.random(2f);
                    spacing = rand.random(2, 3);
                    shields = shieldAmount;
                    shieldScaling = shieldsPerWave;
                }});

                i += next;
                if(curTier < 3 || rand.chance(0.2)){
                    curTier ++;
                }

                //do not spawn bosses
                curTier = Math.min(curTier, 3);

                //small chance to switch species
                if(rand.chance(0.3)){
                    curSpecies = Structs.random(species);
                }
            }
        };

        createProgression.get(0);

        int step = 5 + rand.random(3);

        while(step <= cap){
            createProgression.get(step);
            step += (int)(rand.random(13, 25) * Mathf.lerp(1f, 0.5f, difficulty));
        }

        int bossWave = (int)(rand.random(30, 60) * Mathf.lerp(1f, 0.7f, difficulty));
        int bossSpacing = (int)(rand.random(25, 40) * Mathf.lerp(1f, 0.6f, difficulty));

        //main boss progression
        out.add(new SpawnGroup(Structs.random(species)[4]){{
            unitAmount = 1;
            begin = bossWave;
            spacing = bossSpacing;
            end = never;
            max = 16;
            unitScaling = bossSpacing;
            shieldScaling = shieldsPerWave;
            effect = StatusEffects.boss;
        }});

        //alt boss progression
        out.add(new SpawnGroup(Structs.random(species)[4]){{
            unitAmount = 1;
            begin = bossWave + rand.random(3, 5) * bossSpacing;
            spacing = bossSpacing;
            end = never;
            max = 16;
            unitScaling = bossSpacing;
            shieldScaling = shieldsPerWave;
            effect = StatusEffects.boss;
        }});

        int finalBossStart = 120 + rand.random(30);

        //final boss waves
        out.add(new SpawnGroup(Structs.random(species)[4]){{
            unitAmount = 1;
            begin = finalBossStart;
            spacing = bossSpacing/2;
            end = never;
            unitScaling = bossSpacing;
            shields = 500;
            shieldScaling = shieldsPerWave * 4;
            effect = StatusEffects.boss;
        }});

        //final boss waves (alt)
        out.add(new SpawnGroup(Structs.random(species)[4]){{
            unitAmount = 1;
            begin = finalBossStart + 15;
            spacing = bossSpacing/2;
            end = never;
            unitScaling = bossSpacing;
            shields = 500;
            shieldScaling = shieldsPerWave * 4;
            effect = StatusEffects.boss;
        }});

        //shift back waves on higher difficulty for a harder start
        int shift = Math.max((int)(difficulty * 15 - 5), 0);

        for(SpawnGroup group : out){
            group.begin -= shift;
            group.end -= shift;
        }

        return out;
    }
}
