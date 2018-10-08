package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

/**
 * A BulletType for most ammo-based bullets shot from turrets and units.
 */
public class BasicBulletType extends BulletType{
    public Color backColor = Palette.bulletYellowBack, frontColor = Palette.bulletYellow;
    public float bulletWidth = 5f, bulletHeight = 7f;
    public float bulletShrink = 0.5f;
    public String bulletSprite;

    public int fragBullets = 9;
    public float fragVelocityMin = 0.2f, fragVelocityMax = 1f;
    public BulletType fragBullet = null;

    /**Use a negative value to disable splash damage.*/
    public float splashDamageRadius = -1f;

    public int incendAmount = 0;
    public float incendSpread = 8f;
    public float incendChance = 1f;

    public float homingPower = 0f;
    public float homingRange = 50f;

    public TextureRegion backRegion;
    public TextureRegion frontRegion;

    public float hitShake = 0f;

    public BasicBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage);
        this.bulletSprite = bulletSprite;
    }

    @Override
    public void load(){
        backRegion = Draw.region(bulletSprite + "-back");
        frontRegion = Draw.region(bulletSprite);
    }

    @Override
    public void draw(Bullet b){
        float height = bulletHeight * ((1f - bulletShrink) + bulletShrink * b.fout());

        Draw.color(backColor);
        Draw.rect(backRegion, b.x, b.y, bulletWidth, height, b.angle() - 90);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x, b.y, bulletWidth, height, b.angle() - 90);
        Draw.color();
    }

    @Override
    public void update(Bullet b){
        super.update(b);

        if(homingPower > 0.0001f){
            TargetTrait target = Units.getClosestTarget(b.getTeam(), b.x, b.y, homingRange);
            if(target != null){
                b.getVelocity().setAngle(Angles.moveToward(b.getVelocity().angle(), b.angleTo(target), homingPower * Timers.delta()));
            }
        }
    }

    @Override
    public void hit(Bullet b, float x, float y){
        super.hit(b, x, y);

        Effects.shake(hitShake, hitShake, b);

        if(fragBullet != null){
            for(int i = 0; i < fragBullets; i++){
                float len = Mathf.random(1f, 7f);
                float a = Mathf.random(360f);
                Bullet.create(fragBullet, b, x + Angles.trnsx(a, len), y + Angles.trnsy(a, len), a, Mathf.random(fragVelocityMin, fragVelocityMax));
            }
        }

        if(Mathf.chance(incendChance)){
            Damage.createIncend(x, y, incendSpread, incendAmount);
        }

        if(splashDamageRadius > 0){
            Damage.damage(b.getTeam(), x, y, splashDamageRadius, splashDamage);
        }
    }

    @Override
    public void despawned(Bullet b){
        super.despawned(b);
        if(fragBullet != null || splashDamageRadius > 0){
            hit(b);
        }
    }
}
