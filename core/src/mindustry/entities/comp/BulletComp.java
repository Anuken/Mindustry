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

    @Override
    public void getCollisions(Cons<QuadTree> consumer){
        for(Team team : team.enemies()){
            consumer.get(teamIndex.tree(team));
        }
    }

    @Override
    public void drawBullets(){
        type.draw(base());
    }

    @Override
    public void add(){
        type.init(base());
    }

    @Override
    public void remove(){
        type.despawned(base());
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
        type.hit(base(), x, y);

        if(other instanceof Healthc){
            Healthc h = (Healthc)other;
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
    }

    @Override
    public void update(){
        type.update(base());

        if(type.collidesTiles && type.collides && type.collidesGround){
            world.raycastEach(world.toTile(lastX()), world.toTile(lastY()), tileX(), tileY(), (x, y) -> {

                Building tile = world.build(x, y);
                if(tile == null) return false;

                if(tile.collide(base()) && type.collides(base(), tile) && !tile.dead() && (type.collidesTeam || tile.team != team)){
                    boolean remove = false;

                    if(tile.team != team){
                        remove = tile.collision(base());
                    }

                    if(remove || type.collidesTeam){
                        type.hitTile(base(), tile);
                        remove();
                    }
                    return true;
                }

                return false;
            });
        }
    }

    @Override
    public void draw(){
        Draw.z(Layer.bullet);

        type.draw(base());
        type.drawLight(base());
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
