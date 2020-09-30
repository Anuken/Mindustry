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
@Component(base = true)
abstract class BulletComp implements Timedc, Damagec, Hitboxc, Teamc, Posc, Drawc, Shielderc, Ownerc, Velc, Bulletc, Timerc{
    @Import Team team;
    @Import Entityc owner;
    @Import float x,y;

    IntSeq collided = new IntSeq(6);
    Object data;
    BulletType type;
    float damage;
    float fdata;

    @Override
    public void getCollisions(Cons<QuadTree> consumer){
        if(team.active()){
            for(Team team : team.enemies()){
                consumer.get(teamIndex.tree(team));
            }
        }else{
            for(Team other : Team.all){
                if(other != team && teamIndex.count(other) > 0){
                    consumer.get(teamIndex.tree(other));
                }
            }
        }
    }

    @Override
    public void drawBullets(){
        type.draw(self());
    }

    @Override
    public void add(){
        type.init(self());
    }

    @Override
    public void remove(){
        type.despawned(self());
        collided.clear();
    }

    @Override
    public float damageMultiplier(){
        if(owner instanceof Unit) return ((Unit)owner).damageMultiplier() * state.rules.unitDamageMultiplier;
        if(owner instanceof Building) return state.rules.blockDamageMultiplier;

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
        return type.collides && (other instanceof Teamc && ((Teamc)other).team() != team)
            && !(other instanceof Flyingc && !((Flyingc)other).checkTarget(type.collidesAir, type.collidesGround))
            && !(type.pierce && collided.contains(other.id())); //prevent multiple collisions
    }

    @MethodPriority(100)
    @Override
    public void collision(Hitboxc other, float x, float y){
        type.hit(self(), x, y);
        float health = 0f;

        if(other instanceof Healthc){
            Healthc h = (Healthc)other;
            health = h.health();
            h.damage(damage);
        }

        if(other instanceof Unit){
            Unit unit = (Unit)other;
            unit.impulse(Tmp.v3.set(unit).sub(this.x, this.y).nor().scl(type.knockback * 80f));
            unit.apply(type.status, type.statusDuration);
        }

        //must be last.
        if(!type.pierce){
            remove();
        }else{
            collided.add(other.id());
        }

        type.hitEntity(self(), other, health);
    }

    @Override
    public void update(){
        type.update(self());

        if(type.collidesTiles && type.collides && type.collidesGround){
            world.raycastEach(world.toTile(lastX()), world.toTile(lastY()), tileX(), tileY(), (x, y) -> {

                Building tile = world.build(x, y);
                if(tile == null || !isAdded()) return false;

                if(tile.collide(self()) && type.collides(self(), tile) && !tile.dead() && (type.collidesTeam || tile.team != team) && !(type.pierceBuilding && collided.contains(tile.id))){
                    boolean remove = false;

                    float health = tile.health;

                    if(tile.team != team){
                        remove = tile.collision(self());
                    }

                    if(remove || type.collidesTeam){
                        if(!type.pierceBuilding){
                            remove();
                        }else{
                            collided.add(tile.id);
                        }
                    }

                    type.hitTile(self(), tile, health);

                    return !type.pierceBuilding;
                }

                return false;
            });
        }
    }

    @Override
    public void draw(){
        Draw.z(Layer.bullet);

        type.draw(self());
        type.drawLight(self());
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
