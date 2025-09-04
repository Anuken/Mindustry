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
    /** % of max health for reaching the maximum multiplers. */
    public float minHealth = 0.2f;
    /** Applied slope steepness. */
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
        t.add((effect.hasEmoji() ? effect.emoji() : "") + "[stat]" + effect.localizedName);
        t.row();
        t.add(abilityStat("laststand.minhealth", maxHealth > 0f ? Strings.autoFixed(minHealth * maxHealth, 2) : Strings.autoFixed(minHealth * 100f, 2) + "%"));
        t.row();
        t.add(Core.bundle.format("stat.damagemultiplier", Strings.autoFixed(damageMultiplier * 100, 2)));
        t.row();
        t.add(Core.bundle.format("stat.reloadmultiplier", Strings.autoFixed(reloadMultiplier * 100, 2)));
    }

    @Override
    public void update(Unit unit){
        if(unit.health <= unit.maxHealth){
            float t = Mathf.clamp((1f - unit.health / unit.maxHealth) / (1f - minHealth), 0f, 1f);
            // Might be scuffed, dynamic status is cleaner but requires creating a new (hidden) status effect
            for(var mount : unit.mounts){
            mount.weapon.bullet.damage = mount.weapon.bullet.damage / (damageMultiplier - 1f) * Mathf.pow(t, exponent);
            unit.reloadMultiplier = 1 + (reloadMultiplier - 1f) * Mathf.pow(t, exponent);
            }
            if (unit.health <= unit.maxHealth * minHealth) {
                unit.apply(effect, 5f);
            }
        }
    }
}
