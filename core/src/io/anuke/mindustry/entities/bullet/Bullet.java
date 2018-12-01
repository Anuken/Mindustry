package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.math.Vector2;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.entities.traits.AbsorbTrait;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.entities.traits.TeamTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.BulletEntity;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.entities.trait.VelocityTrait;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;
import io.anuke.ucore.util.Timer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Bullet extends BulletEntity<BulletType> implements TeamTrait, SyncTrait, AbsorbTrait{
    private static Vector2 vector = new Vector2();
    public Timer timer = new Timer(3);
    private float lifeScl;
    private Team team;
    private Object data;
    private boolean supressCollision, supressOnce, initialized;

    /**Internal use only!*/
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
        Bullet bullet = Pooling.obtain(Bullet.class, Bullet::new);
        bullet.type = type;
        bullet.owner = owner;
        bullet.data = data;

        bullet.velocity.set(0, type.speed).setAngle(angle).scl(velocityScl);
        if(type.keepVelocity){
            bullet.velocity.add(owner instanceof VelocityTrait ? ((VelocityTrait) owner).getVelocity() : Vector2.Zero);
        }

        bullet.team = team;
        bullet.type = type;
        bullet.lifeScl = lifetimeScl;

        bullet.set(x - bullet.velocity.x * Timers.delta(), y - bullet.velocity.y * Timers.delta());
        bullet.add();

        return bullet;
    }

    public static Bullet create(BulletType type, Bullet parent, float x, float y, float angle){
        return create(type, parent.owner, parent.team, x, y, angle);
    }

    public static Bullet create(BulletType type, Bullet parent, float x, float y, float angle, float velocityScl){
        return create(type, parent.owner, parent.team, x, y, angle, velocityScl);
    }

    @Remote(called = Loc.server)
    public static void createBullet(BulletType type, float x, float y, float angle){
        create(type, null, Team.none, x, y, angle);
    }

    public boolean collidesTiles(){
        return type.collidesTiles;
    }

    public void supress(){
        supressCollision = true;
        supressOnce = true;
    }

    @Override
    public void absorb(){
        supressCollision = true;
        remove();
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

    @Override
    public float drawSize(){
        return type.drawSize;
    }

    @Override
    public float getDamage(){
        if(owner instanceof Unit){
            return super.getDamage() * ((Unit) owner).getDamageMultipler();
        }

        if(owner instanceof Lightning && data instanceof Float){
            return (Float)data;
        }

        return super.getDamage();
    }

    @Override
    public boolean isSyncing(){
        return type.syncable;
    }

    @Override
    public void write(DataOutput data) throws IOException{
        data.writeFloat(x);
        data.writeFloat(y);
        data.writeFloat(velocity.x);
        data.writeFloat(velocity.y);
        data.writeByte(team.ordinal());
        data.writeByte(type.id);
    }

    @Override
    public void read(DataInput data, long time) throws IOException{
        x = data.readFloat();
        y = data.readFloat();
        velocity.x = data.readFloat();
        velocity.y = data.readFloat();
        team = Team.all[data.readByte()];
        type = content.bullet(data.readByte());
    }

    @Override
    public Team getTeam(){
        return team;
    }

    @Override
    public void draw(){
        type.draw(this);
    }

    @Override
    public float getShieldDamage(){
        return Math.max(getDamage(), type.splashDamage);
    }

    @Override
    public boolean collides(SolidTrait other){
        return type.collides && super.collides(other) && !supressCollision && !(other instanceof Unit && ((Unit) other).isFlying() && !type.collidesAir);
    }

    @Override
    public void collision(SolidTrait other, float x, float y){
        super.collision(other, x, y);

        if(other instanceof Unit){
            Unit unit = (Unit) other;
            unit.getVelocity().add(vector.set(other.getX(), other.getY()).sub(x, y).setLength(type.knockback / unit.getMass()));
            unit.applyEffect(type.status, type.statusIntensity);
        }
    }

    @Override
    public void update(){
        super.update();

        if(type.hitTiles && collidesTiles() && !supressCollision && initialized){
            world.raycastEach(world.toTile(lastPosition().x), world.toTile(lastPosition().y), world.toTile(x), world.toTile(y), (x, y) -> {

                Tile tile = world.tile(x, y);
                if(tile == null) return false;
                tile = tile.target();

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
    protected void updateLife(){
        time += Timers.delta() * 1f/(lifeScl);
        time = Mathf.clamp(time, 0, type.lifetime());

        if(time >= type.lifetime){
            if(!supressCollision) type.despawned(this);
            remove();
        }
    }

    @Override
    public void reset(){
        super.reset();
        timer.clear();
        lifeScl = 1f;
        team = null;
        data = null;
        supressCollision = false;
        supressOnce = false;
        initialized = false;
    }

    @Override
    public void removed(){
        Pooling.free(this);
    }

    @Override
    public EntityGroup targetGroup(){
        return bulletGroup;
    }
}
