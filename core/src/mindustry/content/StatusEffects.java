package mindustry.content;

import arc.*;
import arc.math.Mathf;
import mindustry.entities.Effects;
import mindustry.ctype.ContentList;
import mindustry.game.EventType.*;
import mindustry.type.StatusEffect;
import static mindustry.Vars.*;

public class StatusEffects implements ContentList{
    public static StatusEffect none, burning, freezing, wet, melting, tarred, overdrive, shielded, shocked, corroded, boss;

    @Override
    public void load(){

        none = new StatusEffect("none");

        burning = new StatusEffect("burning"){{
            damage = 0.06f;
            effect = Fx.burning;

            init(() -> {
                opposite(wet,freezing);
                trans(tarred, ((unit, time, newTime, result) -> {
                    unit.damage(1f);
                    Effects.effect(Fx.burning, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
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
            });
        }};

        wet = new StatusEffect("wet"){{
            speedMultiplier = 0.9f;
            effect = Fx.wet;

            init(() -> {
                trans(shocked, ((unit, time, newTime, result) -> {
                    unit.damage(20f);
                    if(unit.getTeam() == state.rules.waveTeam){
                        Events.fire(Trigger.shock);
                    }
                    result.set(this, time);
                }));
                opposite(burning);
            });
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
        }};

        shielded = new StatusEffect("shielded"){{
            armorMultiplier = 3f;
        }};

        boss = new StatusEffect("boss"){{
            armorMultiplier = 3f;
            damageMultiplier = 3f;
            speedMultiplier = 1.1f;
        }};

        shocked = new StatusEffect("shocked");

        //no effects, just small amounts of damage.
        corroded = new StatusEffect("corroded"){{
            damage = 0.1f;
        }};
    }
}
