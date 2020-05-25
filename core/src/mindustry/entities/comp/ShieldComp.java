package mindustry.entities.comp;

import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;

import static mindustry.Vars.minArmorDamage;

@Component
abstract class ShieldComp implements Healthc, Posc{
    @Import float health, hitTime, x, y;
    @Import boolean dead;

    /** Absorbs health damage. */
    float shield;
    /** Absorbs percentage of damage. */
    float armor;
    /** Shield opacity. */
    transient float shieldAlpha = 0f;

    @Replace
    @Override
    public void damage(float amount){
        //apply armor
        amount *= Math.max(1f - armor, minArmorDamage);

        hitTime = 1f;

        boolean hadShields = shield > 0.0001f;

        if(hadShields){
            shieldAlpha = 1f;
        }

        float shieldDamage = Math.min(shield, amount);
        shield -= shieldDamage;
        amount -= shieldDamage;

        if(amount > 0){
            health -= amount;
            if(health <= 0 && !dead){
                kill();
            }

            if(hadShields && shield <= 0.0001f){
                Fx.unitShieldBreak.at(x, y, 0, this);
            }
        }
    }

    @Override
    public void update(){
        shieldAlpha -= Time.delta() / 15f;
        if(shieldAlpha < 0) shieldAlpha = 0f;
    }
}
