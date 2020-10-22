package mindustry.entities.abilities;

import arc.util.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class MovementLightningAbility extends Ability{
    public float damage = 35f, length = 10f, reload = 0.15;
    public float minSpeed = 0.8f, maxSpeed = 1.2f;
    public Effect shootEffect = Fx.lightningShoot;
    
    MovementLightningAbility(){}
    
    public MovementLightningAbility(float damage, float length, float reload, float minSpeed, float maxSpeed){
        this.damage = damage;
        this.length = length;
        this.reload = reload;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }
    
    @Override
    public void update(Unit unit){
        scl = Mathf.clamp((unit.velocity().len() - minSpeed) / (maxSpeed - minSpeed));
        if(Mathf.chance(Time.delta() * (reload * scl))){
            shootEffect.at(unit.x, unit.y, unit.rotation, unit.team.color());
            Lightning.create(unit.team, unit.team.color(), damage, unit.x + unit.velocity().x, unit.y + unit.velocity().y, unit.rotation, length);
        }
    }
}