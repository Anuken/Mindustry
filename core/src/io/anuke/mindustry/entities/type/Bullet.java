package io.anuke.mindustry.entities.type;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.Pool.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class Bullet extends SolidEntity implements DamageTrait, ScaleTrait, Poolable, DrawTrait, VelocityTrait, TimeTrait, TeamTrait, AbsorbTrait{
    public Interval timer = new Interval(3);

    private float lifeScl;
    private Team team;
    private Object data;
    private boolean supressCollision, supressOnce, initialized, deflected;

    protected BulletType type;
    protected Entity owner;
    protected float time;

    /** Internal use only! */
    public Bullet(){
    }

    public static Bullet create(BulletType type, TeamTrait owner, float x, float y, float angle){
        return create(type, owner, owner.getTeam(), x, y, angle);
    }

    public static Bullet create(BulletType type, Entity owner, Team team, float x, float y, float angle){
        return create(type, owner, team, x, y, angle, 1f);
    }

    public static Bullet create(BulletType type, Entity owner, Team team, float x, float y, float angle, float velocityScl){
        return create(type, owner, team, x, y, angle, velocityScl, 1f, null);
    }

    public static Bullet create(BulletType type, Entity owner, Team team, float x, float y, float angle, float velocityScl, float lifetimeScl){
        return create(type, owner, team, x, y, angle, velocityScl, lifetimeScl, null);
    }

    public static Bullet create(BulletType type, Entity owner, Team team, float x, float y, float angle, float velocityScl, float lifetimeScl, Object data){
        Bullet bullet = Pools.obtain(Bullet.class, Bullet::new);
        bullet.type = type;
        bullet.owner = owner;
        bullet.data = data;

        bullet.velocity.set(0, type.speed).setAngle(angle).scl(velocityScl);
        if(type.keepVelocity){
            bullet.velocity.add(owner instanceof VelocityTrait ? ((VelocityTrait)owner).velocity() : Vector2.ZERO);
        }

        bullet.team = team;
        bullet.type = type;
        bullet.lifeScl = lifetimeScl;

        bullet.set(x - bullet.velocity.x * Time.delta(), y - bullet.velocity.y * Time.delta());
        bullet.add();

        return bullet;
    }

    public static Bullet create(BulletType type, Bullet parent, float x, float y, float angle){
        return create(type, parent.owner, parent.team, x, y, angle);
    }

    public static Bullet create(BulletType type, Bullet parent, float x, float y, float angle, float velocityScl){
        return create(type, parent.owner, parent.team, x, y, angle, velocityScl);
    }

    /** Internal use only. */
    @Remote(called = Loc.server, unreliable = true)
    public static void createBullet(BulletType type, float x, float y, float angle){
        create(type, null, Team.derelict, x, y, angle);
    }

    /** ok */
    @Remote(called = Loc.server, unreliable = true)
    public static void createBullet(BulletType type, Team team, float x, float y, float angle){
        create(type, null, team, x, y, angle);
    }

    public Entity getOwner(){
        return owner;
    }

    public boolean collidesTiles(){
        return type.collidesTiles;
    }

    public void deflect(){
        supressCollision = true;
        supressOnce = true;
        deflected = true;
    }

    public boolean isDeflected(){
        return deflected;
    }

    public BulletType getBulletType(){
        return type;
    }

    public void resetOwner(Entity entity, Team team){
        this.owner = entity;
        this.team = team;
    }

    public void scaleTime(float add){
        time += add;
    }

    public Object getData(){
        return data;
    }

    public void setData(Object data){
        this.data = data;
    }

    public float damageMultiplier(){
        if(owner instanceof Unit){
            return ((Unit)owner).getDamageMultipler();
        }
        return 1f;
    }

    @Override
    public void killed(Entity other){
        if(owner instanceof KillerTrait){
            ((KillerTrait)owner).killed(other);
        }
    }

    @Override
    public void absorb(){
        supressCollision = true;
        remove();
    }

    @Override
    public float drawSize(){
        return type.drawSize;
    }

    @Override
    public float damage(){
        if(owner instanceof Lightning && data instanceof Float){
            return (Float)data;
        }
        return type.damage * damageMultiplier();
    }

    @Override
    public Team getTeam(){
        return team;
    }

    @Override
    public float getShieldDamage(){
        return Math.max(damage(), type.splashDamage);
    }

    @Override
    public boolean collides(SolidTrait other){
        return type.collides && (other != owner && !(other instanceof DamageTrait)) && !supressCollision && !(other instanceof Unit && ((Unit)other).isFlying() && !type.collidesAir);
    }

    @Override
    public void collision(SolidTrait other, float x, float y){
        if(!type.pierce) remove();
        type.hit(this, x, y);

        if(other instanceof Unit){
            Unit unit = (Unit)other;
            unit.velocity().add(Tmp.v3.set(other.getX(), other.getY()).sub(x, y).setLength(type.knockback / unit.mass()));
            unit.applyEffect(type.status, type.statusDuration);
        }
    }

    @Override
    public void update(){
        type.update(this);

        x += velocity.x * Time.delta();
        y += velocity.y * Time.delta();

        velocity.scl(Mathf.clamp(1f - type.drag * Time.delta()));

        time += Time.delta() * 1f / (lifeScl);
        time = Mathf.clamp(time, 0, type.lifetime);

        if(time >= type.lifetime){
            if(!supressCollision) type.despawned(this);
            remove();
        }

        if(type.hitTiles && collidesTiles() && !supressCollision && initialized){
            world.raycastEach(world.toTile(lastPosition().x), world.toTile(lastPosition().y), world.toTile(x), world.toTile(y), (x, y) -> {

                Tile tile = world.ltile(x, y);
                if(tile == null) return false;

                if(tile.entity != null && tile.entity.collide(this) && type.collides(this, tile) && !tile.entity.isDead() && (type.collidesTeam || tile.getTeam() != team)){
                    if(tile.getTeam() != team){
                        tile.entity.collision(this);
                    }

                    if(!supressCollision){
                        type.hitTile(this, tile);
                        remove();
                    }

                    return true;
                }

                return false;
            });
        }

        if(supressOnce){
            supressCollision = false;
            supressOnce = false;
        }

        initialized = true;
    }

    @Override
    public void reset(){
        type = null;
        owner = null;
        velocity.setZero();
        time = 0f;
        timer.clear();
        lifeScl = 1f;
        team = null;
        data = null;
        supressCollision = false;
        supressOnce = false;
        deflected = false;
        initialized = false;
    }

    @Override
    public void hitbox(Rectangle rectangle){
        rectangle.setSize(type.hitSize).setCenter(x, y);
    }

    @Override
    public void hitboxTile(Rectangle rectangle){
        rectangle.setSize(type.hitSize).setCenter(x, y);
    }

    @Override
    public float lifetime(){
        return type.lifetime;
    }

    @Override
    public void time(float time){
        this.time = time;
    }

    @Override
    public float time(){
        return time;
    }

    @Override
    public void removed(){
        Pools.free(this);
    }

    @Override
    public EntityGroup targetGroup(){
        return bulletGroup;
    }

    @Override
    public void added(){
        type.init(this);
    }

    @Override
    public void draw(){
        type.draw(this);
    }

    @Override
    public float fin(){
        return time / type.lifetime;
    }

    @Override
    public Vector2 velocity(){
        return velocity;
    }

    public void velocity(float speed, float angle){
        velocity.set(0, speed).setAngle(angle);
    }

    public void limit(float f){
        velocity.limit(f);
    }

    /** Sets the bullet's rotation in degrees. */
    public void rot(float angle){
        velocity.setAngle(angle);
    }

    /** @return the bullet's rotation. */
    public float rot(){
        float angle = Mathf.atan2(velocity.x, velocity.y) * Mathf.radiansToDegrees;
        if(angle < 0) angle += 360;
        return angle;
    }
}
