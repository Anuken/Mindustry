package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.type.*;
import mindustry.graphics.*;


import static mindustry.Vars.*;

public class StatusEffects implements ContentList{
    public static StatusEffect none, burning, freezing, unmoving, slow, wet, muddy, melting, sapped, tarred, overdrive, overclock, shielded, shocked, blasted, corroded, boss, sporeSlowed;

    @Override
    public void load(){

        none = new StatusEffect("none");

        burning = new StatusEffect("burning"){{
            color = Pal.lightFlame;
            damage = 0.12f; //over 8 seconds, this would be ~60 damage
            effect = Fx.burning;

            init(() -> {
                opposite(wet, freezing);
                trans(tarred, ((unit, time, newTime, result) -> {
                    unit.damagePierce(8f);
                    Fx.burning.at(unit.x + Mathf.range(unit.bounds() / 2f), unit.y + Mathf.range(unit.bounds() / 2f));
                    result.set(burning, Math.min(time + newTime, 300f));
                }));
            });
        }};

        freezing = new StatusEffect("freezing"){{
            color = Color.valueOf("6ecdec");
            speedMultiplier = 0.6f;
            healthMultiplier = 0.8f;
            effect = Fx.freezing;

            init(() -> {
                opposite(melting, burning);

                trans(blasted, ((unit, time, newTime, result) -> {
                    unit.damagePierce(18f);
                    result.set(freezing, time);
                }));
            });
        }};

        unmoving = new StatusEffect("unmoving"){{
            color = Pal.gray;
            speedMultiplier = 0.001f;
        }};

        slow = new StatusEffect("slow"){{
            color = Pal.lightishGray;
            speedMultiplier = 0.4f;
        }};

        wet = new StatusEffect("wet"){{
            color = Color.royal;
            speedMultiplier = 0.94f;
            effect = Fx.wet;
            effectChance = 0.09f;

            init(() -> {
                trans(shocked, ((unit, time, newTime, result) -> {
                    unit.damagePierce(14f);
                    if(unit.team == state.rules.waveTeam){
                        Events.fire(Trigger.shock);
                    }
                    result.set(wet, time);
                }));
                opposite(burning);
            });
        }};
		
        muddy = new StatusEffect("muddy"){{
            color = Color.valueOf("46382a");
            speedMultiplier = 0.94f;
            effect = Fx.muddy;
            effectChance = 0.09f;
        }};

        melting = new StatusEffect("melting"){{
            color = Color.valueOf("ffa166");
            speedMultiplier = 0.8f;
            healthMultiplier = 0.8f;
            damage = 0.3f;
            effect = Fx.melting;

            init(() -> {
                opposite(wet, freezing);
                trans(tarred, ((unit, time, newTime, result) -> {
                    unit.damagePierce(8f);
                    Fx.burning.at(unit.x + Mathf.range(unit.bounds() / 2f), unit.y + Mathf.range(unit.bounds() / 2f));
                    result.set(melting, Math.min(time + newTime, 200f));
                }));
            });
        }};

        sapped = new StatusEffect("sapped"){{
            color = Pal.sap;
            speedMultiplier = 0.7f;
            healthMultiplier = 0.8f;
            effect = Fx.sapped;
            effectChance = 0.1f;
        }};

        sporeSlowed = new StatusEffect("spore-slowed"){{
            color = Pal.spore;
            speedMultiplier = 0.8f;
            effect = Fx.sapped;
            effectChance = 0.04f;
        }};

        tarred = new StatusEffect("tarred"){{
            color = Color.valueOf("313131");
            speedMultiplier = 0.6f;
            effect = Fx.oily;

            init(() -> {
                trans(melting, ((unit, time, newTime, result) -> result.set(melting, newTime + time)));
                trans(burning, ((unit, time, newTime, result) -> result.set(burning, newTime + time)));
            });
        }};

        overdrive = new StatusEffect("overdrive"){{
            color = Pal.accent;
            healthMultiplier = 0.95f;
            speedMultiplier = 1.15f;
            damageMultiplier = 1.4f;
            damage = -0.01f;
            effect = Fx.overdriven;
            permanent = true;
        }};

        overclock = new StatusEffect("overclock"){{
            color = Pal.accent;
            speedMultiplier = 1.15f;
            damageMultiplier = 1.15f;
            reloadMultiplier = 1.25f;
            effectChance = 0.07f;
            effect = Fx.overclocked;
        }};

        shielded = new StatusEffect("shielded"){{
            color = Pal.accent;
            healthMultiplier = 3f;
        }};

        boss = new StatusEffect("boss"){{
            color = Pal.health;
            permanent = true;
            damageMultiplier = 1.3f;
            healthMultiplier = 1.5f;
        }};

        shocked = new StatusEffect("shocked"){{
            color = Pal.lancerLaser;
            reactive = true;
        }};

        blasted = new StatusEffect("blasted"){{
            color = Color.valueOf("ff795e");
            reactive = true;
        }};

        corroded = new StatusEffect("corroded"){{
            color = Pal.plastanium;
            damage = 0.1f;
        }};
    }
}
