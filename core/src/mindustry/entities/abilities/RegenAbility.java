package mindustry.entities.abilities;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;

public class RegenAbility extends Ability{
    /** Amount healed as percent per tick. */
    public float percentAmount = 0f;
    /** Amount healed as a flat amount per tick. */
    public float amount = 0f;

    @Override
    public void addStats(Table t){
        super.addStats(t);

        boolean flat = amount >= 0.001f;
        boolean percent = percentAmount >= 0.001f;

        if(flat || percent){
            t.add(abilityStat("regen",
                (flat ? Strings.autoFixed(amount * 60f, 2) + (percent ? " [lightgray]+[stat] " : "") : "")
                    + (percent ? Strings.autoFixed(percentAmount * 60f, 2) + "%" : "")
            ));
        }
    }

    @Override
    public void update(Unit unit){
        unit.heal((unit.maxHealth * percentAmount / 100f + amount) * Time.delta);
    }
}
