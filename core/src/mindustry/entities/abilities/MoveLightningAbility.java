package mindustry.entities.abilities;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import arc.audio.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class MoveLightningAbility extends Ability{
    /** Lightning damage */
    public float damage = 35f;
    /** Chance of firing every tick. Set >= 1 to always fire lightning every tick at max speed. */
    public float chance = 0.15f;
    /** Length of the lightning */
    public int length = 12;
    /** Speeds for when to start lightninging and when to stop getting faster */
    public float minSpeed = 0.8f, maxSpeed = 1.2f;
    /** Offset shoot location vertically */
    public float offset = 0f;
    /** Lightning color */
    public Color color = Color.valueOf("a9d8ff");
    
    public Effect shootEffect = Fx.sparkShoot;
    public Sound shootSound = Sounds.spark;
    
    MoveLightningAbility(){}
    
    public MoveLightningAbility(float damage, int length, float chance, float minSpeed, float maxSpeed, float offset, Color color){
        this.damage = damage;
        this.length = length;
        this.chance = chance;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.offset = offset.
    }
    
    @Override
    public void update(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        if(Mathf.chance(Time.delta * chance * scl)){
            shootEffect.at(unit.x + unit.vel.x + Angles.trnsx(unit.rotation, offset, 0), unit.y + unit.vel.y + Angles.trnsx(unit.rotation, offset, 0), unit.rotation, color);
            Lightning.create(unit.team, color, damage, unit.x + unit.vel.x + Angles.trnsx(unit.rotation, offset, 0), unit.y + unit.vel.y + Angles.trnsx(unit.rotation, offset, 0), unit.rotation, length);
            shootSound.at(unit);
        }
    }
}
