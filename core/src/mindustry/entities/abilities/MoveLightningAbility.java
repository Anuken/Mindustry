package mindustry.entities.abilities;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

public class MoveLightningAbility extends Ability{
    /** Lightning damage */
    public float damage = 35f;
    /** Chance of firing every tick. Set >= 1 to always fire lightning every tick at max speed. */
    public float chance = 0.15f;
    /** Length of the lightning. <= 0 to disable */
    public int length = 12;
    /** Speeds for when to start lightninging and when to stop getting faster */
    public float minSpeed = 0.8f, maxSpeed = 1.2f;
    /** Lightning color */
    public Color color = Color.valueOf("a9d8ff");
    /** Shifts where the lightning spawns along the Y axis */
    public float offset = 0f;
    /** Offset along the X axis. */
    public float width = 0f;
    /** Jittering heat sprite like the shield on v5 Javelin */
    public String heatRegion = "error";
    /** Bullet type that is fired. Can be null */
    public @Nullable BulletType bullet;
    /** Bullet angle parameters */
    public float bulletAngle = 0f, bulletSpread = 0f;
    
    public Effect shootEffect = Fx.sparkShoot;
    public Sound shootSound = Sounds.spark;

    protected float side = 1f;
    
    MoveLightningAbility(){}
    
    public MoveLightningAbility(float damage, int length, float chance, float offset, float minSpeed, float maxSpeed, Color color, String heatRegion){
        this.damage = damage;
        this.length = length;
        this.chance = chance;
        this.offset = offset;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.heatRegion = heatRegion;
    }
    
    public MoveLightningAbility(float damage, int length, float chance, float offset, float minSpeed, float maxSpeed, Color color){
        this.damage = damage;
        this.length = length;
        this.chance = chance;
        this.offset = offset;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.color = color;
    }
    
    @Override
    public void update(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        if(Mathf.chance(Time.delta * chance * scl)){
            float x = unit.x + Angles.trnsx(unit.rotation, offset, width * side), y = unit.y + Angles.trnsy(unit.rotation, offset, width * side);

            shootEffect.at(x, y, unit.rotation, color);
            shootSound.at(unit);

            if(length > 0){
                Lightning.create(unit.team, color, damage, x + unit.vel.x, y + unit.vel.y, unit.rotation, length);
            }

            if(bullet != null){
                bullet.create(unit, unit.team, x, y, unit.rotation + bulletAngle + Mathf.range(bulletSpread));
            }

            side *= -1f;
        }
    }
    
    @Override
    public void draw(Unit unit){
        float scl = Mathf.clamp((unit.vel().len() - minSpeed) / (maxSpeed - minSpeed));
        TextureRegion region = Core.atlas.find(heatRegion);
        if(Core.atlas.isFound(region) && scl > 0.00001f){
            Draw.color(color);
            Draw.alpha(scl / 2f);
            Draw.blend(Blending.additive);
            Draw.rect(region, unit.x + Mathf.range(scl / 2f), unit.y + Mathf.range(scl / 2f), unit.rotation - 90);
            Draw.blend();
        }
    }
}
