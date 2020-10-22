package mindustry.entities.abilities;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import arc.audio.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class MovementLightningAbility extends Ability{
    //Lightning damage
    public float damage = 35f;
    //Ticks between firing
    public float reload = 15f;
    //Length of the lightning
    public int length = 12;
    //Speeds for when to start lightninging and when to stop getting faster
    public float minSpeed = 0.8f, maxSpeed = 1.2f;
    //Lightning Color
    public Color color = Color.valueOf("a9d8ff");
    
    public Effect shootEffect = Fx.sparkShoot;
    public Sound shootSound = Sounds.spark;
    
    MovementLightningAbility(){}
    
    public MovementLightningAbility(float damage, int length, float reload, float minSpeed, float maxSpeed, Color color){
        this.damage = damage;
        this.length = length;
        this.reload = reload;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
    }
    
    @Override
    public void update(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        if(Mathf.chance(Time.delta * (reload/10 * scl))){
            shootEffect.at(unit.x, unit.y, unit.rotation, color);
            Lightning.create(unit.team, color, damage, unit.x + unit.vel().x, unit.y + unit.vel().y, unit.rotation, length);
            shootSound.at(unit.x, unit.y);
        }
    }
}