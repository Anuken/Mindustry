package mindustry.entities.abilities;

import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import java.util.*;

import static mindustry.Vars.*;

public class RepairFieldAbility extends Ability{
    public float amount = 1, reload = 100, range = 60, healPercent = 0f;
    public Effect healEffect = Fx.heal;
    public Effect activeEffect = Fx.healWaveDynamic;
    public Sound sound = Sounds.healWave;
    public float soundVolume = 0.5f;
    public boolean parentizeEffects = false;
    /** Multiplies healing to units of the same type by this amount. */
    public float sameTypeHealMult = 1f;
    /** Maximum number of units healed. */
    public int maxTargets = -1;

    /** If true, this ability will consider missing hp, number of targets, cooldowns...etc. */
    public boolean smartHeal = false;
    /** If a damaged unit health % is lower than this heal it as soon as possible. Values close to 1f will likely waste healing potential. */
    public float smartHealPercent = 0f;
    /** A multiplier on potential healing efficiency. High values -> efficient aoe healing. Low values -> efficient single target healing. */
    public float smartHealStrength = 1f;
    /** If all damaged units havent been hurt for at least this amount of time, force healing them, regardless of thresholds. */
    public float smartDowntime = 60f * 8f;
    /** How often to check for healing when at least 1 damaged unit is found. */
    public float smartInterval = 20f;
    /** Random initial reload multiplier to check for damaged units. Useful for desyncing healer timers once. */
    public float randDesync = -1f;

    //a high capacity is needed to account for t1 spam in custom gamemodes, but set too high it becomes expensive
    protected static Seq<Unit> targets = new Seq<>(50);
    protected float healTimer, downTimer;
    protected float healthMissing, sumMaxHealth, sumTypeMult, healthChange;
    protected boolean hasHealed, healNow;

    public RepairFieldAbility(){}

    public RepairFieldAbility(float amount, float reload, float range){
        this.amount = amount;
        this.reload = reload;
        this.range = range;
    }
    public RepairFieldAbility(float amount, float reload, float range, float healPercent){
        this.amount = amount;
        this.reload = reload;
        this.range = range;
        this.healPercent = healPercent;
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("bullet.range", Strings.autoFixed(range / tilesize, 2)));
        t.row();
        t.add(abilityStat("firingrate", Strings.autoFixed(60f / reload, 2)));
        t.row();
        if(amount > 0){
            t.add(Core.bundle.format("bullet.healamount", Strings.autoFixed(amount, 2)) + "[lightgray] ~ []" + abilityStat("repairspeed", Strings.autoFixed(amount * 60f / reload, 2)));
            t.row();
        }
        if(healPercent > 0f){
            t.row();
            t.add(Core.bundle.format("bullet.healpercent", Strings.autoFixed(healPercent, 2)) + "[lightgray] ~ []" + abilityStat("repairspeed", Strings.autoFixed(healPercent * 60f / reload, 2) + "%"));
        }
        if(sameTypeHealMult != 1f){
            t.row();
            t.add(abilityStat("sametypehealmultiplier", (sameTypeHealMult < 1f ? "[negstat]" : "") + Strings.autoFixed(sameTypeHealMult * 100f, 2)));
        }
        if(maxTargets > 0){
            t.row();
            t.add(abilityStat("maxtargets", maxTargets));
        }
    }

    @Override
    public void update(Unit unit){
        healTimer += Time.delta;
        downTimer = smartHeal && healthChange >= healthMissing && healthMissing > 0f ? downTimer + Time.delta : 0f;

        if(healTimer >= reload){
            targets.clear();
            hasHealed = healNow = false;
            healthChange = healthMissing;
            healthMissing = sumMaxHealth = sumTypeMult = 0f;

            boolean limitTargets = maxTargets >= 0;
            float healPercentMult = healPercent / 100f;

            Units.nearby(unit.team, unit.x, unit.y, range, other -> {
                //check for 2 more targets just in case
                if(limitTargets && targets.size >= maxTargets + 2) return;

                if(other.damaged()){
                    targets.add(other);
                    if(smartHeal){
                        float maxHealth = other.maxHealth();
                        healthMissing += maxHealth - other.health();
                        sumMaxHealth += maxHealth;
                        sumTypeMult += unit.type == other.type ? sameTypeHealMult : 1f;
                        if(other.healthf() < smartHealPercent) healNow = true;
                    }
                }
            });
            int targetCount = targets.size;

            //mixed approach, care both about groups and single low hp units
            float ratio = amount + healPercentMult * sumMaxHealth * (sumTypeMult / targetCount);
            float requiredHeals = (healthMissing * 0.7f + healthMissing / (limitTargets ? maxTargets : targetCount) * 0.3f) / smartHealStrength / ratio;

            if(requiredHeals >= 1f || !smartHeal || healNow || downTimer >= smartDowntime){

                //sort closest if number of targets is limited
                if(limitTargets){
                    boolean isSameType = sameTypeHealMult < 1f;
                    targets.sort(u -> u.dst2(unit.x, unit.y) + (isSameType && u.type() == unit.type ? 6400f : 0f));
                }

                int len = limitTargets ? Math.min(targetCount, maxTargets) : targetCount;
                for(int i = 0; i < len; i++){
                    Unit other = targets.get(i);
                    if(other.damaged()){
                        float maxHealth = other.maxHealth();
                        float healMult = unit.type == other.type ? sameTypeHealMult : 1f;
                        other.heal((amount + healPercentMult * maxHealth) * healMult);
                        healEffect.at(other, parentizeEffects);
                        hasHealed = true;
                    }
                }
                if(hasHealed){
                    healTimer = 0f;
                    activeEffect.at(unit, range);
                    sound.at(unit, 1f + Mathf.range(0.1f), soundVolume);
                }

            //increase how often this checks if there are damaged units but still below the healing threshold
            }else if(smartHeal && targetCount > 0){
                healTimer = reload >= (2f * smartInterval) ? reload - smartInterval : smartInterval;
            }else if(randDesync > 0){
                healTimer = Mathf.random(randDesync) * reload;
            }else{
                healTimer = 0;
            }
        }
    }
}