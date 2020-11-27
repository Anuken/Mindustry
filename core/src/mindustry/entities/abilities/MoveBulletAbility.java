package mindustry.entities.abilities;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import arc.audio.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

public class MoveBulletAbility extends Ability{
    //Lightning damage
    public float damage = 35f;
    //Chance of firing every tick. Set >= 1 to always fire bullet every tick at max speed.
    public float chance = 0.15f;
    //Length of the lightning
    public int length = 12;
    //Bullet of unit that he shoot
    public BulletType bullet = Bullets.damageLightning;
    //If you need lightning just write true
    public boolean lightning = false;
    //Speeds for when to start lightninging and when to stop getting faster
    public float minSpeed = 0.8f, maxSpeed = 1.2f;
    //Effect color
    public Color color = Color.valueOf("a9d8ff");
    
    public Effect shootEffect = Fx.sparkShoot;
    public Sound shootSound = Sounds.spark;
    
    MoveBulletAbility(){}
    
    public MoveBulletAbility(BulletType bullet, boolean lightning, Effect shootEffect, Sound shootSound, float damage, float chance, int length, float minSpeed, float maxSpeed, Color color){
        this.bullet = bullet;
        this.lightning = lightning;
        this.shootEffect = shootEffect;
        this.shootSound = shootSound;
        this.damage = damage;
        this.chance = chance;
        this.length = length;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
    }
    
    @Override
    public void update(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        if(Mathf.chance(Time.delta * chance * scl)){
            shootEffect.at(unit.x, unit.y, unit.rotation, color);
            bullet.create(unit, unit.team, unit.x, unit.y, unit.rotation);
            shootSound.at(unit);
            if(lightning){
                Fx.sparkShoot.at(unit.x, unit.y, unit.rotation, color);
                Lightning.create(unit.team, color, damage, unit.x + unit.vel.x, unit.y + unit.vel.y, unit.rotation, length);
                Sounds.spark.at(unit);
            }
        }
    }
}
