package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Unit;
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
import io.anuke.ucore.util.Pooling;
import io.anuke.ucore.util.Timer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.bulletGroup;
import static io.anuke.mindustry.Vars.world;

public class Bullet extends BulletEntity<BulletType> implements TeamTrait, SyncTrait{
    private static Vector2 vector = new Vector2();
    public Timer timer = new Timer(3);
    private Team team;
    private Object data;
    private boolean supressCollision;

    /**Internal use only!*/
    public Bullet(){
    }

    public static void create(BulletType type, TeamTrait owner, float x, float y, float angle){
        create(type, owner, owner.getTeam(), x, y, angle);
    }

    public static void create(BulletType type, Entity owner, Team team, float x, float y, float angle){
        create(type, owner, team, x, y, angle, 1f);
    }

    public static void create(BulletType type, Entity owner, Team team, float x, float y, float angle, float velocityScl){
        create(type, owner, team, x, y, angle, velocityScl, null);
    }

    public static void create(BulletType type, Entity owner, Team team, float x, float y, float angle, float velocityScl, Object data){
        Bullet bullet = Pooling.obtain(Bullet.class);
        bullet.type = type;
        bullet.owner = owner;
        bullet.data = data;

        bullet.velocity.set(0, type.speed).setAngle(angle).scl(velocityScl);
        if(type.keepVelocity){
            bullet.velocity.add(owner instanceof VelocityTrait ? ((VelocityTrait) owner).getVelocity() : Vector2.Zero);
        }
        bullet.hitbox.setSize(type.hitsize);

        bullet.team = team;
        bullet.type = type;

        //translate bullets backwards, purely for visual reasons
        float backDelta = Timers.delta();

        bullet.lastPosition().set(x - bullet.velocity.x * backDelta, y - bullet.velocity.y * backDelta, bullet.angle());
        bullet.setLastUpdated(TimeUtils.millis());
        bullet.setUpdateSpacing((long) ((Timers.delta() / 60f) * 1000));
        bullet.set(x - bullet.velocity.x * backDelta, y - bullet.velocity.y * backDelta);

        bullet.add();
    }

    public static void create(BulletType type, Bullet parent, float x, float y, float angle){
        create(type, parent.owner, parent.team, x, y, angle);
    }

    public static void create(BulletType type, Bullet parent, float x, float y, float angle, float velocityScl){
        create(type, parent.owner, parent.team, x, y, angle, velocityScl);
    }

    @Remote(called = Loc.server)
    public static void createBullet(BulletType type, float x, float y, float angle){
        create(type, null, Team.none, x, y, angle);
    }

    public boolean collidesTiles(){
        return type.collidesTiles;
    }

    public void supressCollision(){
        supressCollision = true;
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

    @Override
    public float getDamage(){
        if(owner instanceof Unit){
            return super.getDamage() * ((Unit) owner).getDamageMultipler();
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
        type = BulletType.getByID(data.readByte());
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
    public float drawSize(){
        return 8;
    }

    @Override
    public boolean collides(SolidTrait other){
        return type.collides && super.collides(other);
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

        if(type.hitTiles && collidesTiles() && !supressCollision){
            world.raycastEach(world.toTile(lastPosition().x), world.toTile(lastPosition().y), world.toTile(x), world.toTile(y), (x, y) -> {

                Tile tile = world.tile(x, y);
                if(tile == null) return false;
                tile = tile.target();

                if(tile.entity != null && tile.entity.collide(this) && !tile.entity.isDead() && (type.collidesTeam || tile.entity.tile.getTeam() != team)){
                    if(tile.entity.getTeam() != team){
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

        supressCollision = false;
    }

    @Override
    public void reset(){
        super.reset();
        timer.clear();
        team = null;
        data = null;
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
