package mindustry.entities.abilities;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class LastStandAbility extends Ability{
    public StatusEffect effect = StatusEffects.none;
    public float maxHealth;
    /** Has support for both <1 and >1 values. */
    public float damageMultiplier = 1f, reloadMultiplier = 1f, speedMultiplier = 1f, rotateSpeedMultiplier = 1f, weaponRotateMultiplier = 1f;
    /** % of max health for reaching the maximum multipliers. */
    public float minHealth = 0.2f;
    /** Applied slope steepness. Higher values equal harder to achieve max boost. */
    public float exponent = 2f;
    protected float warmup;

    public TextureRegion shineRegion;
    public String shineSuffix = "-shine";
    public Color color = Pal.turretHeat;
    public float z = -1;

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
            new StatEntry("maxspeedmultiplier", speedMultiplier, effect.speedMultiplier),
            new StatEntry("maxrotatespeedmultiplier", rotateSpeedMultiplier, effect.rotateSpeedMultiplier),
            new StatEntry("maxweaponrotatemultiplier", weaponRotateMultiplier, effect.weaponRotateMultiplier)
        );

        for(StatEntry s : stats){
            if(s.value > 0f && s.value != 1f){
                String text = (s.value < 1f ?  "[negstat]" : "") + Strings.autoFixed(s.value * 100f, 2);
                if(s.effectValue != 1f && effect != StatusEffects.none){
                    text += "%" + (s.effectValue > 1f ? "[stat] + " : "[negstat] ") + Strings.autoFixed((s.effectValue - 1f) * 100f, 2);
                }
                t.add(abilityStat(s.name, text));
                t.row();
            }
        }
    }
    
    @Override
    public void init(UnitType type){
        maxHealth = type.health;
        shineRegion = Core.atlas.find(type.name + shineSuffix, type.region);
    } 

    @Override
    public void update(Unit unit){
        if(unit.health <= unit.maxHealth){
            warmup = Mathf.clamp((1f - unit.health / unit.maxHealth) / (1f - minHealth), 0f, 1f);
            // I am unsure if this is a good way to implement this...
            if(damageMultiplier != 1f) unit.damageMultiplier *= 1f + (damageMultiplier - 1f) * Mathf.pow(warmup, exponent);
            if(reloadMultiplier != 1f) unit.reloadMultiplier *= 1f + (reloadMultiplier - 1f) * Mathf.pow(warmup, exponent);
            if(speedMultiplier != 1f) unit.speedMultiplier *= 1f + (speedMultiplier - 1f) * Mathf.pow(warmup, exponent);
            if(rotateSpeedMultiplier != 1f) unit.rotateSpeedMultiplier *= 1f + (rotateSpeedMultiplier - 1f) * Mathf.pow(warmup, exponent);
            if(weaponRotateMultiplier != 1f) unit.weaponRotateMultiplier *= 1f + (weaponRotateMultiplier - 1f) * Mathf.pow(warmup, exponent);

            if (unit.health <= unit.maxHealth * minHealth) {
                unit.apply(effect, 5f);
            }
        }
    }

    @Override
    public void draw(Unit unit){
        if(shineRegion.found() && warmup > 0.001f){
            float pz = Draw.z();
            if(z > 0) Draw.z(z);
            Draw.color(color, warmup);
            Draw.blend(Blending.additive);
            Draw.alpha(Mathf.absin(Time.time, 2f / warmup, warmup / 2f + 0.5f));
            Draw.rect(shineRegion, unit.x, unit.y, unit.rotation - 90f);
            Draw.blend();
            Draw.color();
            Draw.z(pz);
        }
    }
}