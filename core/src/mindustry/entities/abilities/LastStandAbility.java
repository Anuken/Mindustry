package mindustry.entities.abilities;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

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
    public float shineLayer = -1;
    public Color color = Pal.turretHeat;
    public @Nullable Effect effect = Fx.overHeatParticle;
    public float effectLayer = Layer.groundUnit + 0.01f;

    public static class StatEntry{
        public String name;
        public float value;
        public float effectValue;

        public StatEntry(String name, float value, float effectValue){
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

        //consider boosteffect multiplier in stats
        var stats = Seq.with(
            new StatEntry("maxdamagemultiplier", damageMultiplier, statusEffect.damageMultiplier),
            new StatEntry("maxreloadmultiplier", reloadMultiplier, statusEffect.reloadMultiplier),
            new StatEntry("maxspeedmultiplier", speedMultiplier, statusEffect.speedMultiplier),
            new StatEntry("maxrotatespeedmultiplier", rotateSpeedMultiplier, statusEffect.rotateSpeedMultiplier)
        );

        for(StatEntry s : stats){
            if(s.value > 0f && s.value != 1f){
                String text = StatValues.multStat(s.value, false);
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

            //I am unsure if this is a good way to implement this...
            if(damageMultiplier != 1f) unit.damageMultiplier *= scaleMult(damageMultiplier, warmup);
            if(reloadMultiplier != 1f) unit.reloadMultiplier *= scaleMult(reloadMultiplier, warmup);
            if(speedMultiplier != 1f) unit.speedMultiplier *= scaleMult(speedMultiplier, warmup);
            if(rotateSpeedMultiplier != 1f) unit.rotateSpeedMultiplier *= scaleMult(rotateSpeedMultiplier, warmup);

            if(effect != null && Mathf.chanceDelta(warmup * 0.3f)){
                Tmp.v1.rnd(Mathf.range(unit.type.hitSize * 0.75f));
                effect.at(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, effectLayer, color, unit);
            }

            if(unit.health <= unit.maxHealth * minHealth && statusEffect != StatusEffects.none){
                unit.apply(statusEffect, 5f);
            }
        }
    }

    public float scaleMult(float mult, float warmup){
        return 1f + (mult - 1f) * warmup;
    }

    @Override
    public void draw(Unit unit){
        if(drawShine){
            shineRegion = Core.atlas.find(unit.type.name + shineSuffix, unit.type.region);

            if(shineRegion.found() && warmup > 0.001f){
                if(shineLayer > 0) Draw.z(shineLayer);
                Draw.color(color, warmup);
                Draw.blend(Blending.additive);
                Draw.alpha(Mathf.absin(Vars.state.time, 2f / (warmup * shineSpeed), warmup / 2f + 0.5f));
                Draw.rect(shineRegion, unit.x, unit.y, unit.rotation - 90f);
                Draw.reset();
            }
        }
    }
}