package mindustry.entities.abilities;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class ShieldRegenFieldAbility extends Ability{
    public float amount = 1, max = 100f, reload = 100, range = 60;
    public Effect applyEffect = Fx.shieldApply;
    public Effect activeEffect = Fx.shieldWave;
    public boolean parentizeEffects;

    protected float timer;
    protected boolean applied = false;

    ShieldRegenFieldAbility(){}

    public ShieldRegenFieldAbility(float amount, float max, float reload, float range){
        this.amount = amount;
        this.max = max;
        this.reload = reload;
        this.range = range;
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("bullet.range", Strings.autoFixed(range / tilesize, 2)));
        t.row();
        t.add(abilityStat("firingrate", Strings.autoFixed(60f / reload, 2)));
        t.row();
        t.add(abilityStat("shield", Strings.autoFixed(max, 2)));
    }

    @Override
    public void update(Unit unit){
        timer += Time.delta;

        if(timer >= reload){
            applied = false;

            Units.nearby(unit.team, unit.x, unit.y, range, other -> {
                if(other.shield < max){
                    other.shield = Math.min(other.shield + amount, max);
                    other.shieldAlpha = 1f; //TODO may not be necessary
                    applyEffect.at(other.x, other.y, 0f, other.type.shieldColor(other), parentizeEffects ? other : null);
                    applied = true;
                }
            });

            if(applied){
                activeEffect.at(unit.x, unit.y, unit.type.shieldColor(unit));
            }

            timer = 0f;
        }
    }
}
