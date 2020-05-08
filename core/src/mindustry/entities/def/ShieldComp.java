package mindustry.entities.def;

import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class ShieldComp implements Healthc{
    @Import float health, hitTime;
    @Import boolean dead;

    /** Absorbs health damage. */
    float shield;

    @Replace
    @Override
    public void damage(float amount){
        hitTime = 1f;

        float shieldDamage = Math.min(shield, amount);
        shield -= shieldDamage;
        amount -= shieldDamage;

        if(amount > 0){
            health -= amount;
            if(health <= 0 && !dead){
                kill();
            }
        }
    }
}
