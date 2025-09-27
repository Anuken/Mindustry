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
    public StatusEffect statusEffect = StatusEffects.none;
    public float maxHealth;
    /** Has support for both <1 and >1 values. */
    public float damageMultiplier = 1f, reloadMultiplier = 1f, speedMultiplier = 1f, rotateSpeedMultiplier = 1f;
    /** % of max health for reaching the maximum multipliers. */
    public float minHealth = 0.2f;
    /** Applied slope steepness. Higher values equal harder to achieve max boost. */
    public float exponent = 2f;
    protected float warmup;

    public TextureRegion shineRegion;
    public String shineSuffix = "-shine";
    public boolean drawShine = true;
    public float shineSpeed = 1f;
    public float z = -1;
    public Color color = Pal.turretHeat;
    public @Nullable Effect effect = Fx.regenSuppressParticle;

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
        if(statusEffect != StatusEffects.none){
        t.add((statusEffect.hasEmoji() ? statusEffect.emoji() : "") + "[stat]" + statusEffect.localizedName + abilityStat("maxboosteffect"));
        t.row();
        }
            
        // Consider boosteffect multiplier in stats
        stats = Seq.with(
            new StatEntry("maxdamagemultiplier", damageMultiplier, statusEffect.damageMultiplier),
            new StatEntry("maxreloadmultiplier", reloadMultiplier, statusEffect.reloadMultiplier),
            new StatEntry("maxspeedmultiplier", speedMultiplier, statusEffect.speedMultiplier),
            new StatEntry("maxrotatespeedmultiplier", rotateSpeedMultiplier, statusEffect.rotateSpeedMultiplier)
        );

        for(StatEntry s : stats){
            if(s.value > 0f && s.value != 1f){
                String text = (s.value < 1f ?  "[negstat]" : "") + Strings.autoFixed(s.value * 100f, 2);
                if(s.effectValue != 1f && statusEffect != StatusEffects.none){
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
    } 

    @Override
    public void update(Unit unit){
        if(unit.health <= unit.maxHealth){
            warmup = Mathf.pow(Mathf.clamp((1f - unit.health / unit.maxHealth) / (1f - minHealth), 0f, 1f), exponent);

            // I am unsure if this is a good way to implement this...
            if(damageMultiplier != 1f) unit.damageMultiplier *= 1f + (damageMultiplier - 1f) * warmup;
            if(reloadMultiplier != 1f) unit.reloadMultiplier *= 1f + (reloadMultiplier - 1f) * warmup;
            if(speedMultiplier != 1f) unit.speedMultiplier *= 1f + (speedMultiplier - 1f) * warmup;
            if(rotateSpeedMultiplier != 1f) unit.rotateSpeedMultiplier *= 1f + (rotateSpeedMultiplier - 1f) * warmup;

            if(effect != null && Mathf.chanceDelta(warmup * 0.3f)){
                Tmp.v1.rnd(Mathf.range(unit.type.hitSize * 0.8f));
                effect.at(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, 0, color, unit);
            }

            if(unit.health <= unit.maxHealth * minHealth) {
                unit.apply(statusEffect, 5f);
            }
        }
    }

    @Override
    public void draw(Unit unit){
        if(drawShine){
            shineRegion = Core.atlas.find(unit.type.name + shineSuffix, unit.type.region);

            if(shineRegion.found() && warmup > 0.001f){
                float pz = Draw.z();
                if(z > 0) Draw.z(z);
                Draw.color(color, warmup);
                Draw.blend(Blending.additive);
                Draw.alpha(Mathf.absin(Time.time, 2f / (warmup * shineSpeed), warmup / 2f + 0.5f));
                Draw.rect(shineRegion, unit.x, unit.y, unit.rotation - 90f);
                Draw.blend();
                Draw.color();
                Draw.z(pz);
            }
        }
    }
}