package mindustry.entities.abilities;

import arc.util.*;
import mindustry.gen.*;

public class RegenAbility extends Ability{
    /** Amount healed as percent per tick. */
    public float percentAmount = 0f;
    /** Amount healed as a flat amount per tick. */
    public float amount = 0f;

    @Override
    public void update(Unit unit){
        unit.heal((unit.maxHealth * percentAmount / 100f + amount) * Time.delta);
    }
}
