package io.anuke.mindustry.content;

import io.anuke.arc.*;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.type.StatusEffect;

import static io.anuke.mindustry.Vars.waveTeam;

public class StatusEffects implements ContentList{
    public static StatusEffect none, burning, freezing, wet, melting, tarred, overdrive, shielded, shocked, corroded, boss;

    @Override
    public void load(){

        none = new StatusEffect();

        burning = new StatusEffect(){{
            damage = 0.04f;
            effect = Fx.burning;

            opposite(() -> wet, () -> freezing);
            trans(() -> tarred, ((unit, time, newTime, result) -> {
                unit.damage(1f);
                Effects.effect(Fx.burning, unit.x + Mathf.range(unit.getSize() / 2f), unit.y + Mathf.range(unit.getSize() / 2f));
                result.set(this, Math.min(time + newTime, 300f));
            }));
        }};

        freezing = new StatusEffect(){{
            speedMultiplier = 0.6f;
            armorMultiplier = 0.8f;
            effect = Fx.freezing;

            opposite(() -> melting, () -> burning);
        }};

        wet = new StatusEffect(){{
            speedMultiplier = 0.9f;
            effect = Fx.wet;

            trans(() -> shocked, ((unit, time, newTime, result) -> {
                unit.damage(20f);
                if(unit.getTeam() == waveTeam){
                    Events.fire(Trigger.shock);
                }
                result.set(this, time);
            }));
            opposite(() -> burning);
        }};

        melting = new StatusEffect(){{
            speedMultiplier = 0.8f;
            armorMultiplier = 0.8f;
            damage = 0.3f;
            effect = Fx.melting;

            trans(() -> tarred, ((unit, time, newTime, result) -> result.set(this, Math.min(time + newTime / 2f, 140f))));
            opposite(() -> wet, () -> freezing);
        }};

        tarred = new StatusEffect(){{
            speedMultiplier = 0.6f;
            effect = Fx.oily;

            trans(() -> melting, ((unit, time, newTime, result) -> result.set(burning, newTime + time)));
            trans(() -> burning, ((unit, time, newTime, result) -> result.set(burning, newTime + time)));
        }};

        overdrive = new StatusEffect(){{
            armorMultiplier = 0.95f;
            speedMultiplier = 1.15f;
            damageMultiplier = 1.4f;
            damage = -0.01f;
            effect = Fx.overdriven;
        }};

        shielded = new StatusEffect(){{
            armorMultiplier = 3f;
        }};

        boss = new StatusEffect(){{
            armorMultiplier = 3f;
            damageMultiplier = 3f;
            speedMultiplier = 1.1f;
        }};

        shocked = new StatusEffect();

        //no effects, just small amounts of damage.
        corroded = new StatusEffect(){{
            damage = 0.1f;
        }};
    }
}
