package mindustry.entities.abilities;

import arc.util.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import static mindustry.Vars.*;

public class MovementLightningAbility extends Ability{
    public float damage = 35f, reload = 0.15f;
    public int length = 12f;
    public float minSpeed = 0.8f, maxSpeed = 1.2f;
    public Effect shootEffect = Fx.lightningShoot;
    
    MovementLightningAbility(){}
    
    public MovementLightningAbility(float damage, int length, float reload, float minSpeed, float maxSpeed){
        this.damage = damage;
        this.length = length;
        this.reload = reload;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }
    
    @Override
    public void update(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        if(Mathf.chance(Time.delta * (reload * scl))){
            shootEffect.at(unit.x, unit.y, unit.rotation, unit.team.color);
            Lightning.create(unit.team, unit.team.color, damage * Vars.state.rules.playerDamageMultiplie, unit.x + unit.vel().x, unit.y + unit.vel().y, unit.rotation, length);
        }
    }
}