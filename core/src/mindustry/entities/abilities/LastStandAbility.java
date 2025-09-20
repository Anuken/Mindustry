package mindustry.entities.abilities;

import arc.*;
import arc.math.Mathf;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

public class LastStandAbility extends Ability{
    public StatusEffect effect = StatusEffects.none;
    public float maxHealth;
    public float damageMultiplier = 1f, reloadMultiplier = 1f, speedMultiplier = 1f, rotateSpeedMultiplier = 1f, weaponRotateMultiplier = 1f;
    /** % of max health for reaching the maximum multipliers. */
    public float minHealth = 0.2f;
    /** Applied slope steepness. Higher values equal harder to achieve max boost */
    public float exponent = 2f;
    public Seq<StatEntry> stats;

    public class StatEntry{
        public String name;
        public float value;
        public float effectValue;

        public StatEntry (String name, float value, float effectValue){
            this.name = name;
            this.value = value;
            this.effectValue = effectValue;
        }
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(abilityStat("minhealthboost", maxHealth > 0f ? Strings.autoFixed(minHealth * maxHealth, 2) : Strings.autoFixed(minHealth * 100f, 2) + "%"));
        t.row();
        if(effect != StatusEffects.none){
        t.add((effect.hasEmoji() ? effect.emoji() : "") + "[stat]" + effect.localizedName + abilityStat("maxboosteffect"));
        t.row();
        }
            
        // Consider boosteffect multiplier in stats
        stats = Seq.with(
            new StatEntry("maxdamagemultiplier", damageMultiplier, effect.damageMultiplier),
            new StatEntry("maxreloadmultiplier", reloadMultiplier, effect.reloadMultiplier),
            new StatEntry("maxspeedmultiplier",  speedMultiplier, effect.speedMultiplier),
            new StatEntry("maxrotatespeedmultiplier",  rotateSpeedMultiplier, effect.rotateSpeedMultiplier),
            new StatEntry("maxweaponrotatemultiplier",  weaponRotateMultiplier, effect.weaponRotateMultiplier)
        );

        for(StatEntry s : stats){
            if(s.value > 0f && s.value != 1f){
                String text = Strings.autoFixed(s.value * 100, 2);
                if(s.effectValue != 1f && effect != StatusEffects.none){
                    text += "%" + (s.effectValue > 1f ? " + " : " - ") + Strings.autoFixed((s.effectValue - 1f) * 100f, 2);
                }
                t.add(abilityStat(s.name, text));
                t.row();
            }
        }
    }
    
    @Override
    public void init(UnitType type){
        maxHealth = type.health;
    } 

    @Override
    public void update(Unit unit){
        if(unit.health <= unit.maxHealth){
            float t = Mathf.clamp((1f - unit.health / unit.maxHealth) / (1f - minHealth), 0f, 1f);
            // I am unsure if this is a good way to implement this...
            if(damageMultiplier != 1f) unit.damageMultiplier *= 1f + (damageMultiplier - 1f) * Mathf.pow(t, exponent);
            if(reloadMultiplier != 1f) unit.reloadMultiplier *= 1f + (reloadMultiplier - 1f) * Mathf.pow(t, exponent);
            if(speedMultiplier != 1f) unit.speedMultiplier *= 1f + (speedMultiplier - 1f) * Mathf.pow(t, exponent);
            if(rotateSpeedMultiplier != 1f) unit.rotateSpeedMultiplier *= 1f + (rotateSpeedMultiplier - 1f) * Mathf.pow(t, exponent);
            if(weaponRotateMultiplier != 1f) unit.weaponRotateMultiplier *= 1f + (weaponRotateMultiplier - 1f) * Mathf.pow(t, exponent);

            if (unit.health <= unit.maxHealth * minHealth) {
                unit.apply(effect, 5f);
            }
        }
    }
}