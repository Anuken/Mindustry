package mindustry.entities.abilities;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.audio.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class MoveLightningAbility extends Ability{
    //Lightning damage
    public float damage = 35f;
    //Chance of firing every tick. Set >= 1 to always fire lightning every tick at max speed.
    public float chance = 0.15f;
    //Length of the lightning
    public int length = 12;
    //Speeds for when to start lightninging and when to stop getting faster
    public float minSpeed = 0.8f, maxSpeed = 1.2f;
    //Lightning color
    public Color color = Color.valueOf("a9d8ff");
    //Shifts where the lightning spawns along the Y axis
    public float offset = 0f;
    //Jittering heat sprite like the shield on v5 Javelin
    public TextureRegion heatRegion;
    
    public Effect shootEffect = Fx.sparkShoot;
    public Sound shootSound = Sounds.spark;
    
    MoveLightningAbility(){}
    
    public MoveLightningAbility(float damage, int length, float chance, float offset, float minSpeed, float maxSpeed, Color color, TextureRegion heatRegion){
        this.damage = damage;
        this.length = length;
        this.chance = chance;
        this.offset = offset;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.heatRegion = heatRegion;
    }
    
    public MoveLightningAbility(float damage, int length, float chance, float offset, float minSpeed, float maxSpeed, Color color, String heatName){
        this.damage = damage;
        this.length = length;
        this.chance = chance;
        this.offset = offset;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.heatRegion = Core.atlas.find(heatName);
    }
    
    public MoveLightningAbility(float damage, int length, float chance, float offset, float minSpeed, float maxSpeed, Color color){
        this.damage = damage;
        this.length = length;
        this.chance = chance;
        this.offset = offset;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.heatRegion = Core.atlas.find("error");
    }
    
    @Override
    public void update(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        if(Mathf.chance(Time.delta * chance * scl)){
            float x = unit.x + Angles.trnsx(unit.rotation, offset, 0), y = unit.y + Angles.trnsy(unit.rotation, offset, 0);
            shootEffect.at(x, y, unit.rotation, color);
            Lightning.create(unit.team, color, damage, x + unit.vel.x, y + unit.vel.y, unit.rotation, length);
            shootSound.at(unit);
        }
    }
    
    @Override
    public void draw(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        if(heatRegion != Core.atlas.find("error") && scl > 0.00001f){
            Draw.color(color);
            Draw.alpha(scl / 2f);
            Draw.blend(Blending.additive);
            Draw.rect(heatRegion, unit.x + Mathf.range(scl / 2f), unit.y + Mathf.range(scl / 2f), unit.rotation - 90);
            Draw.blend();
        }
    }
}
