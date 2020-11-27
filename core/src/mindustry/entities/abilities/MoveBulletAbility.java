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
    public BulletType bulletType = Bullets.damageLightning;
    //If you need lightning just write true
    public boolean bullet = false;
    //If you need lightning just write true
    public boolean lightning = false;
    //Speeds for when to start lightninging and when to stop getting faster
    public float minSpeed = 0.8f, maxSpeed = 1.2f;
    //Effect color
    public Color color = Color.valueOf("a9d8ff");
    
    public Effect shootEffect = Fx.sparkShoot;
    public Sound shootSound = Sounds.spark;
    
    MoveBulletAbility(){}
    
    public MoveBulletAbility(BulletType bulletType, boolean bullet, Effect shootEffect, Sound shootSound, float chance, float minSpeed, float maxSpeed, Color color){
        this.bulletType = bulletType;
        this.bullet = bullet;
        abilityStats(shootEffect, shootSound, chance, minSpeed, maxSpeed, color);
    }

    public MoveBulletAbility(boolean lightning, Effect shootEffect, Sound shootSound, float damage, float chance, int length, float minSpeed, float maxSpeed, Color color){
        this.lightning = lightning;
        this.damage = damage;
        this.length = length;
        abilityStats(shootEffect, shootSound, chance, minSpeed, maxSpeed, color);
    }

    public MoveBulletAbility(BulletType bulletType, boolean bullet, boolean lightning, Effect shootEffect, Sound shootSound, float damage, float chance, int length, float minSpeed, float maxSpeed, Color color){
        this.bulletType = bulletType;
        this.lightning = lightning;
        this.bullet = bullet;
        this.damage = damage;
        this.length = length;
        abilityStats(shootEffect, shootSound, chance, minSpeed, maxSpeed, color);
    }

    public void abilityStats(Effect shootEffect, Sound shootSound, float chance, float minSpeed, float maxSpeed, Color color){
        this.shootEffect = shootEffect;
        this.shootSound = shootSound;
        this.chance = chance;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
    }

    @Override
    public void update(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        if(Mathf.chance(Time.delta * scl * (chance * 2))){
            if(bullet){
                shootEffect.at(unit.x, unit.y, unit.rotation, color);
                bulletType.create(unit, unit.team, unit.x, unit.y, unit.rotation);
                shootSound.at(unit);
            }
            if(lightning && Mathf.chance(chance / 2)){
                Lightning.create(unit.team, color, damage, unit.x + unit.vel.x, unit.y + unit.vel.y, unit.rotation, length);
            }
        }
    }
}
