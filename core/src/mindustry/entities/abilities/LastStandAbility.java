package mindustry.entities.abilities;

import arc.*;
import arc.math.Mathf;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class LastStandAbility extends Ability{
    public StatusEffect effect = StatusEffects.overclock;
    public float damageMultiplier = 2f;
    public float reloadMultiplier = 2f;
    /** % of max health for reaching the maximum multiplers. */
    public float minHealth = 0.2f;
    /** Applied slope steepness. */
    public float exponent = 2f;

    LastStandAbility(){}

    public LastStandAbility(StatusEffect effect, float damageMultiplier, float reloadMultiplier, float minHealth){
        this.effect = effect;
        this.damageMultiplier = damageMultiplier;
        this.reloadMultiplier = reloadMultiplier;
        this.minHealth = minHealth;
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add((effect.hasEmoji() ? effect.emoji() : "") + "[stat]" + effect.localizedName);
        t.row();
        t.add(abilityStat("ability.laststand.stat.minhealth", Strings.autoFixed(minHealth * type.health, 2)));
        t.row();
        t.add(abilityStat("stat.damagemultiplier", Strings.autoFixed(damageMultiplier, 2)));
        t.row();
        t.add(abilityStat("stat.reloadmultiplier", Strings.autoFixed(reloadMultiplier, 2)));
    }

    @Override
    public void update(Unit unit){
        if(unit.health <= unit.maxHealth){
            float t = Mathf.clamp((1f - unit.health / unit.maxHealth) / (1f - minHealth), 0f, 1f);

            super.damageMultiplier((damageMultiplier - 1f) * Mathf.pow(t, exponent));
            unit.reloadMultiplier((reloadMultiplier - 1f) * Mathf.pow(t, exponent));
        }
        if(unit.health <= unit.maxHealth * minHealth) {
            unit.apply(effect, 5f);
        }
    }
}
