package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.math.Mathf;
import mindustry.ctype.ContentList;
import mindustry.game.EventType.*;
import mindustry.type.StatusEffect;
import static mindustry.Vars.*;

public class StatusEffects implements ContentList{
    public static StatusEffect none, burning, freezing, wet, muddy, melting, sapped, tarred, overdrive, overclock, shielded, shocked, blasted, corroded, boss, sporeSlowed;

    @Override
    public void load(){

        none = new StatusEffect("none");

        burning = new StatusEffect("burning"){{
            damage = 0.12f; //over 8 seconds, this would be 60 damage
            effect = Fx.burning;

            init(() -> {
                opposite(wet,freezing);
                trans(tarred, ((unit, time, newTime, result) -> {
                    unit.damagePierce(8f);
                    Fx.burning.at(unit.x() + Mathf.range(unit.bounds() / 2f), unit.y() + Mathf.range(unit.bounds() / 2f));
                    result.set(this, Math.min(time + newTime, 300f));
                }));
            });
        }};

        freezing = new StatusEffect("freezing"){{
            speedMultiplier = 0.6f;
            armorMultiplier = 0.8f;
            effect = Fx.freezing;

            init(() -> {
                opposite(melting, burning);

                trans(blasted, ((unit, time, newTime, result) -> {
                    unit.damagePierce(18f);
                    result.set(this, time);
                }));
            });
        }};

        wet = new StatusEffect("wet"){{
            color = Color.royal;
            speedMultiplier = 0.94f;
            effect = Fx.wet;
            effectChance = 0.09f;

            init(() -> {
                trans(shocked, ((unit, time, newTime, result) -> {
                    unit.damagePierce(14f);
                    if(unit.team() == state.rules.waveTeam){
                        Events.fire(Trigger.shock);
                    }
                    result.set(this, time);
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
            speedMultiplier = 0.8f;
            armorMultiplier = 0.8f;
            damage = 0.3f;
            effect = Fx.melting;

            init(() -> {
                trans(tarred, ((unit, time, newTime, result) -> result.set(this, Math.min(time + newTime / 2f, 140f))));
                opposite(wet, freezing);
            });
        }};

        sapped = new StatusEffect("sapped"){{
            speedMultiplier = 0.7f;
            armorMultiplier = 0.8f;
            effect = Fx.sapped;
            effectChance = 0.1f;
        }};

        sporeSlowed = new StatusEffect("spore-slowed"){{
            speedMultiplier = 0.8f;
            effect = Fx.sapped;
            effectChance = 0.04f;
        }};

        tarred = new StatusEffect("tarred"){{
            speedMultiplier = 0.6f;
            effect = Fx.oily;

            init(() -> {
                trans(melting, ((unit, time, newTime, result) -> result.set(burning, newTime + time)));
                trans(burning, ((unit, time, newTime, result) -> result.set(burning, newTime + time)));
            });
        }};

        overdrive = new StatusEffect("overdrive"){{
            armorMultiplier = 0.95f;
            speedMultiplier = 1.15f;
            damageMultiplier = 1.4f;
            damage = -0.01f;
            effect = Fx.overdriven;
            permanent = true;
        }};

        overclock = new StatusEffect("overclock"){{
            speedMultiplier = 1.15f;
            damageMultiplier = 1.15f;
            reloadMultiplier = 1.25f;
            effectChance = 0.07f;
            effect = Fx.overclocked;
        }};

        shielded = new StatusEffect("shielded"){{
            armorMultiplier = 3f;
        }};

        boss = new StatusEffect("boss"){{
            permanent = true;
            damageMultiplier = 1.5f;
            armorMultiplier = 1.5f;
        }};

        shocked = new StatusEffect("shocked");

        blasted = new StatusEffect("blasted");

        corroded = new StatusEffect("corroded"){{
            damage = 0.1f;
        }};
    }
}
