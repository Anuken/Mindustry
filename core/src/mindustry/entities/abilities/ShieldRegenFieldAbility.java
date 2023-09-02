package mindustry.entities.abilities;

import arc.Core;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

import static mindustry.Vars.tilesize;

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
        t.add("[lightgray]" + Core.bundle.get("waves.shields") + ": [white]" + Math.round(max)); //extremely stupid usage
        t.row();
        t.add("[lightgray]" + Stat.shootRange.localized() + ": [white]" +  Strings.autoFixed(range / tilesize, 2) + " " + StatUnit.blocks.localized());
        t.row();
        t.add("[lightgray]" + Stat.reload.localized() + ": [white]" + Strings.autoFixed(60f / reload, 2) + " " + StatUnit.perSecond.localized());
        t.row();
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
                    applyEffect.at(unit.x, unit.y, 0f, unit.team.color, parentizeEffects ? other : null);
                    applied = true;
                }
            });

            if(applied){
                activeEffect.at(unit.x, unit.y, unit.team.color);
            }

            timer = 0f;
        }
    }
}
