package mindustry.entities.abilities;

import arc.*;
import arc.math.Mathf;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

public class LastStandAbility extends Ability{
    public StatusEffect effect = StatusEffects.overclock;
    public float maxHealth;
    public float damageMultiplier = 2f;
    public float reloadMultiplier = 2f;
    /** % of max health for reaching the maximum multipliers. */
    public float minHealth = 0.2f;
    /** Applied slope steepness */
    public float exponent = 2f;

    LastStandAbility(){}

    /** Set maxHealth to unit health as a workaround */
    public LastStandAbility(StatusEffect effect, float damageMultiplier, float reloadMultiplier, float minHealth, float maxHealth){
        this.effect = effect;
        this.damageMultiplier = damageMultiplier;
        this.reloadMultiplier = reloadMultiplier;
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(abilityStat("minhealthboost", maxHealth > 0f ? Strings.autoFixed(minHealth * maxHealth, 2) : Strings.autoFixed(minHealth * 100f, 2) + "%"));
        t.row();
        t.add((effect.hasEmoji() ? effect.emoji() : "") + "[stat]" + effect.localizedName + abilityStat("maxboosteffect"));
        t.row();
        t.add(abilityStat("maxdamagemultiplier", Strings.autoFixed(damageMultiplier * 100, 2) + (effect.damageMultiplier > 0f ? "% + " + Strings.autoFixed((effect.damageMultiplier - 1f) * 100f, 2) : "")));
        t.row();
        t.add(abilityStat("maxreloadmultiplier", Strings.autoFixed(reloadMultiplier * 100, 2) + (effect.reloadMultiplier > 0f ? "% + " + Strings.autoFixed((effect.reloadMultiplier - 1f) * 100f, 2) : "")));
    }

    @Override
    public void update(Unit unit){
        if(unit.health <= unit.maxHealth){
            float t = Mathf.clamp((1f - unit.health / unit.maxHealth) / (1f - minHealth), 0f, 1f);
            unit.damageMultiplier = 1f + (damageMultiplier - 1f) * Mathf.pow(t, exponent);
            unit.reloadMultiplier = 1f + (reloadMultiplier - 1f) * Mathf.pow(t, exponent);

            if (unit.health <= unit.maxHealth * minHealth) {
                unit.apply(effect, 5f);
            }
        }
    }
}
