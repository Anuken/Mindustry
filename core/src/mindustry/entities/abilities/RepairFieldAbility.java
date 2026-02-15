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
    /** Maximum number of units healed. */
    public int maxTargets = -1;
    public Effect activeEffect = Fx.healWaveDynamic;
    public Sound sound = Sounds.healWave;
    public float soundVolume = 0.5f;
    public boolean parentizeEffects = false;
    /** Multiplies healing to units of the same type by this amount. */
    public float sameTypeHealMult = 1f;

    protected float timer;
    private static final Seq<Unit> all = new Seq<>();
    protected boolean wasHealed = false;
    protected int targets;

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
        t.add(abilityStat("firingrate", Strings.autoFixed(60f / reload, 2)));
        t.row();
        if(amount > 0){
            t.add(Core.bundle.format("bullet.healamount", Strings.autoFixed(amount, 2)) + "[lightgray] ~ []" + abilityStat("repairspeed", Strings.autoFixed(amount * 60f / reload, 2)));
            t.row();
        }       
        if(maxTargets > 0){
            t.add(abilityStat("maxtargets", maxTargets));
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
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta;

        if(timer >= reload){
            wasHealed = false;

            all.clear();
            Units.nearby(unit.team, unit.x, unit.y, range, other -> {
                if(other.damaged()){
                    all.add(other);
                }
            });
            all.sort(u -> u.dst2(unit.x, unit.y) + ((sameTypeHealMult < 1f && u.type() == unit.type)? 6400f : 0f));
            int len = Math.min(all.size, (maxTargets > -1)? maxTargets : all.size);

            for(int i = 0; i < len; i++){
                Unit other = all.get(i);
                if(other.damaged()){
                    healEffect.at(other, parentizeEffects);
                    wasHealed = true;
                }
                float healMult = unit.type == other.type() ? sameTypeHealMult : 1f;
                other.heal((amount + healPercent / 100f * other.maxHealth()) * healMult);
            }

            if(wasHealed){
                activeEffect.at(unit, range);
                sound.at(unit, 1f + Mathf.range(0.1f), soundVolume);
            }

            timer = 0f;
        }
    }
}
