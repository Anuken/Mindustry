package mindustry.entities.abilities;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class RepairFieldAbility extends Ability{
    public float amount = 1, reload = 100, range = 60, healPercent = 0f;
    public Effect healEffect = Fx.heal;
    public Effect activeEffect = Fx.healWaveDynamic;
    public boolean parentizeEffects = false;
    /** Multiplies healing to units of the same type by this amount. */
    public float sameTypeHealMult = 1f;

    protected float timer;
    protected boolean wasHealed = false;

    RepairFieldAbility(){}

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
        t.add(abilityStat("repairspeed", Strings.autoFixed(amount * 60f / reload, 2)));
        t.row();
        if(healPercent > 0f){
            t.row();
            t.add(Core.bundle.format("bullet.healpercent", Strings.autoFixed(healPercent, 2)));
        }
        if(sameTypeHealMult != 1f){
            t.row();
            t.add(abilityStat("sametypehealmultiplier", (sameTypeHealMult < 1f ? "[negstat]" : "") + Strings.autoFixed(sameTypeHealMult * 100f, 2)));
        }
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta;

        if(timer >= reload){
            wasHealed = false;

            Units.nearby(unit.team, unit.x, unit.y, range, other -> {
                if(other.damaged()){
                    healEffect.at(other, parentizeEffects);
                    wasHealed = true;
                }
                float healMult = unit.type == other.type ? sameTypeHealMult : 1f;
                other.heal((amount + healPercent / 100f * other.maxHealth()) * healMult);
            });

            if(wasHealed){
                activeEffect.at(unit, range);
            }

            timer = 0f;
        }
    }
}
