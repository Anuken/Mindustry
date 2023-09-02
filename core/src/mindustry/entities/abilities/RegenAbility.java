package mindustry.entities.abilities;

import arc.Core;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

public class RegenAbility extends Ability{
    /** Amount healed as percent per tick. */
    public float percentAmount = 0f;
    /** Amount healed as a flat amount per tick. */
    public float amount = 0f;

    @Override
    public void addStats(Table t){
        if(amount > 0.01f){
            t.add("[lightgray]" + Stat.repairSpeed.localized() + ": [white]" + Strings.autoFixed(amount * 60f, 2) + StatUnit.perSecond.localized());
            t.row();
        }

        if(percentAmount > 0.01f){
            t.add(Core.bundle.format("bullet.healpercent", Strings.autoFixed(percentAmount * 60f, 2)) + StatUnit.perSecond.localized()); //stupid but works
        }
    }

    @Override
    public void update(Unit unit){
        unit.heal((unit.maxHealth * percentAmount / 100f + amount) * Time.delta);
    }
}
