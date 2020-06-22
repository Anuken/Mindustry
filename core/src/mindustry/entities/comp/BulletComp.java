package mindustry.entities.comp;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

@EntityDef(value = {Bulletc.class}, pooled = true, serialize = false)
@Component
abstract class BulletComp implements Timedc, Damagec, Hitboxc, Teamc, Posc, Drawc, Shielderc, Ownerc, Velc, Bulletc, Timerc{
    @Import Team team;
    @Import Entityc owner;

    IntSeq collided = new IntSeq(6);
    Object data;
    BulletType type;
    float damage;

    @Override
    public void getCollisions(Cons<QuadTree> consumer){
        for(Team team : state.teams.enemiesOf(team)){
            consumer.get(teamIndex.tree(team));
        }
    }

    @Override
    public void drawBullets(){
        type.draw(this);
    }

    @Override
    public void add(){
        type.init(this);
    }

    @Override
    public void remove(){
        type.despawned(this);
        collided.clear();
    }

    @Override
    public float damageMultiplier(){
        if(owner instanceof Unitc) return ((Unitc)owner).damageMultiplier() * state.rules.unitDamageMultiplier;
        if(owner instanceof Tilec) return state.rules.blockDamageMultiplier;

        return 1f;
    }

    @Override
    public void absorb(){
        remove();
    }

    @Replace
    public float clipSize(){
        return type.drawSize;
    }

    @Override
    public float damage(){
        return damage * damageMultiplier();
    }

    @Replace
    @Override
    public boolean collides(Hitboxc other){
        return type.collides && (other instanceof Teamc && ((Teamc)other).team() != team())
            && !(other instanceof Flyingc && ((((Flyingc)other).isFlying() && !type.collidesAir) || (((Flyingc)other).isGrounded() && !type.collidesGround)))
            && !(type.pierce && collided.contains(other.id())); //prevent multiple collisions
    }

    @MethodPriority(100)
    @Override
    public void collision(Hitboxc other, float x, float y){
        type.hit(this, x, y);

        if(other instanceof Healthc){
            Healthc h = (Healthc)other;
            h.damage(damage);
        }

        if(other instanceof Unitc){
            Unitc unit = (Unitc)other;
            unit.vel().add(Tmp.v3.set(other.x(), other.y()).sub(x, y).setLength(type.knockback / unit.mass()));
            unit.apply(type.status, type.statusDuration);
        }

        //must be last.
        if(!type.pierce){
            remove();
        }else{
            collided.add(other.id());
        }
    }

    @Override
    public void update(){
        type.update(this);

        if(type.collidesTiles && type.collides){
            world.raycastEach(world.toTile(lastX()), world.toTile(lastY()), tileX(), tileY(), (x, y) -> {

                Tilec tile = world.ent(x, y);
                if(tile == null) return false;

                if(tile.collide(this) && type.collides(this, tile) && !tile.dead() && (type.collidesTeam || tile.team() != team())){
                    if(tile.team() != team()){
                        tile.collision(this);
                    }

                    type.hitTile(this, tile);
                    remove();
                    return true;
                }

                return false;
            });
        }
    }

    @Override
    public void draw(){
        Draw.z(Layer.bullet);

        type.draw(this);
        type.drawLight(this);
    }

    /** Sets the bullet's rotation in degrees. */
    @Override
    public void rotation(float angle){
        vel().setAngle(angle);
    }

    /** @return the bullet's rotation. */
    @Override
    public float rotation(){
        float angle = Mathf.atan2(vel().x, vel().y) * Mathf.radiansToDegrees;
        if(angle < 0) angle += 360;
        return angle;
    }
}
