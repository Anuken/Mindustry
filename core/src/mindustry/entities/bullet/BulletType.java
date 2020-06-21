package mindustry.entities.bullet;

import arc.audio.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public abstract class BulletType extends Content{
    public float lifetime;
    public float speed;
    public float damage;
    public float hitSize = 4;
    public float drawSize = 40f;
    public float drag = 0f;
    public boolean pierce;
    public Effect hitEffect, despawnEffect;

    /** Effect created when shooting. */
    public Effect shootEffect = Fx.shootSmall;
    /** Extra smoke effect created when shooting. */
    public Effect smokeEffect = Fx.shootSmallSmoke;
    /** Sound made when hitting something or getting removed.*/
    public Sound hitSound = Sounds.none;
    /** Extra inaccuracy when firing. */
    public float inaccuracy = 0f;
    /** How many bullets get created per ammo item/liquid. */
    public float ammoMultiplier = 2f;
    /** Multiplied by turret reload speed to get final shoot speed. */
    public float reloadMultiplier = 1f;
    /** Multiplier of how much base damage is done to tiles. */
    public float tileDamageMultiplier = 1f;
    /** Recoil from shooter entities. */
    public float recoil;
    /** Whether to kill the shooter when this is shot. For suicide bombers. */
    public boolean killShooter;
    /** Whether to instantly make the bullet disappear. */
    public boolean instantDisappear;
    /** Damage dealt in splash. 0 to disable.*/
    public float splashDamage = 0f;
    /** Knockback in velocity. */
    public float knockback;
    /** Status effect applied on hit. */
    public StatusEffect status = StatusEffects.none;
    /** Intensity of applied status effect in terms of duration. */
    public float statusDuration = 60 * 10f;
    /** Whether this bullet type collides with tiles. */
    public boolean collidesTiles = true;
    /** Whether this bullet type collides with tiles that are of the same team. */
    public boolean collidesTeam = false;
    /** Whether this bullet type collides with air/ground units. */
    public boolean collidesAir = true, collidesGround = true;
    /** Whether this bullet types collides with anything at all. */
    public boolean collides = true;
    /** Whether velocity is inherited from the shooter. */
    public boolean keepVelocity = true;
    /** Whether to scale velocity to disappear at the target position. Used for artillery. */
    public boolean scaleVelocity;
    /** Whether this bullet can be hit by point defense. */
    public boolean hittable = true;

    //additional effects

    public float fragCone = 360f;
    public int fragBullets = 9;
    public float fragVelocityMin = 0.2f, fragVelocityMax = 1f;
    public BulletType fragBullet = null;
    public Color hitColor = Color.white;

    /** Use a negative value to disable splash damage. */
    public float splashDamageRadius = -1f;

    public int incendAmount = 0;
    public float incendSpread = 8f;
    public float incendChance = 1f;
    public float homingPower = 0f;
    public float homingRange = 50f;

    public int lightning;
    public int lightningLength = 5;
    /** Use a negative value to use default bullet damage. */
    public float lightningDamage = -1;

    public float weaveScale = 1f;
    public float weaveMag = -1f;
    public float hitShake = 0f;

    public float lightRadius = 16f;
    public float lightOpacity = 0.3f;
    public Color lightColor = Pal.powerLight;

    public BulletType(float speed, float damage){
        this.speed = speed;
        this.damage = damage;
        lifetime = 40f;
        hitEffect = Fx.hitBulletSmall;
        despawnEffect = Fx.hitBulletSmall;
    }

    /** Returns maximum distance the bullet this bullet type has can travel. */
    public float range(){
        return speed * lifetime * (1f - drag);
    }

    public boolean collides(Bulletc bullet, Tilec tile){
        return true;
    }

    public void hitTile(Bulletc b, Tilec tile){
        hit(b);
    }

    public void hit(Bulletc b){
        hit(b, b.getX(), b.getY());
    }

    public void hit(Bulletc b, float x, float y){
        hitEffect.at(x, y, b.rotation(), hitColor);
        hitSound.at(b);

        Effects.shake(hitShake, hitShake, b);

        if(fragBullet != null){
            for(int i = 0; i < fragBullets; i++){
                float len = Mathf.random(1f, 7f);
                float a = b.rotation() + Mathf.range(fragCone/2);
                fragBullet.create(b, x + Angles.trnsx(a, len), y + Angles.trnsy(a, len), a, Mathf.random(fragVelocityMin, fragVelocityMax));
            }
        }

        if(Mathf.chance(incendChance)){
            Damage.createIncend(x, y, incendSpread, incendAmount);
        }

        if(splashDamageRadius > 0){
            Damage.damage(b.team(), x, y, splashDamageRadius, splashDamage * b.damageMultiplier(), collidesAir, collidesGround);

            if(status != StatusEffects.none){
                Damage.status(b.team(), x, y, splashDamageRadius, status, statusDuration, collidesAir, collidesGround);
            }
        }

        for(int i = 0; i < lightning; i++){
            Lightning.create(b.team(), Pal.surge, lightningDamage < 0 ? damage : lightningDamage, b.getX(), b.getY(), Mathf.random(360f), lightningLength);
        }
    }

    public void despawned(Bulletc b){
        despawnEffect.at(b.getX(), b.getY(), b.rotation());
        hitSound.at(b);

        if(fragBullet != null || splashDamageRadius > 0 || lightning > 0){
            hit(b);
        }
    }

    public void draw(Bulletc b){
    }

    public void drawLight(Bulletc b){
        Drawf.light(b.team(), b, lightRadius, lightColor, lightOpacity);
    }

    public void init(Bulletc b){
        if(killShooter && b.owner() instanceof Healthc){
            ((Healthc)b.owner()).kill();
        }

        if(instantDisappear){
            b.time(lifetime);
        }
    }

    public void update(Bulletc b){
        if(homingPower > 0.0001f){
            Teamc target = Units.closestTarget(b.team(), b.getX(), b.getY(), homingRange, e -> (e.isGrounded() && collidesGround) || (e.isFlying() && collidesAir), t -> collidesGround);
            if(target != null){
                b.vel().setAngle(Mathf.slerpDelta(b.rotation(), b.angleTo(target), homingPower));
            }
        }

        if(weaveMag > 0){
            b.vel().rotate(Mathf.sin(Time.time() + b.id() * 3, weaveScale, weaveMag) * Time.delta());
        }
    }

    @Override
    public ContentType getContentType(){
        return ContentType.bullet;
    }

    public Bulletc create(Teamc owner, float x, float y, float angle){
        return create(owner, owner.team(), x, y, angle);
    }

    public Bulletc create(Entityc owner, Team team, float x, float y, float angle){
        return create(owner, team, x, y, angle, 1f);
    }

    public Bulletc create(Entityc owner, Team team, float x, float y, float angle, float velocityScl){
        return create(owner, team, x, y, angle, -1, velocityScl, 1f, null);
    }

    public Bulletc create(Entityc owner, Team team, float x, float y, float angle, float velocityScl, float lifetimeScl){
        return create(owner, team, x, y, angle, -1, velocityScl, lifetimeScl, null);
    }

    public Bulletc create(Bulletc parent, float x, float y, float angle){
        return create(parent.owner(), parent.team(), x, y, angle);
    }

    public Bulletc create(Bulletc parent, float x, float y, float angle, float velocityScl){
        return create(parent.owner(), parent.team(), x, y, angle, velocityScl);
    }

    public Bulletc create(@Nullable Entityc owner, Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl, Object data){
        Bulletc bullet = BulletEntity.create();
        bullet.type(this);
        bullet.owner(owner);
        bullet.team(team);
        bullet.vel().trns(angle, speed * velocityScl);
        bullet.set(x - bullet.vel().x * Time.delta(), y - bullet.vel().y * Time.delta());
        bullet.lifetime(lifetime * lifetimeScl);
        bullet.data(data);
        bullet.drag(drag);
        bullet.hitSize(hitSize);
        bullet.damage(damage < 0 ? this.damage : damage);
        bullet.add();

        if(keepVelocity && owner instanceof Hitboxc) bullet.vel().add(((Hitboxc)owner).deltaX(), ((Hitboxc)owner).deltaY());
        return bullet;

    }

    public void createNet(Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl){
        Call.createBullet(this, team, x, y, damage, angle, velocityScl, lifetimeScl);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void createBullet(BulletType type, Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl){
        type.create(null, team, x, y, angle, damage, velocityScl, lifetimeScl, null);
    }
}
