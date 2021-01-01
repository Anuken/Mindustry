package mindustry.entities.comp;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.bullet.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.Wall.*;

import static mindustry.Vars.*;

@EntityDef(value = {Bulletc.class}, pooled = true, serialize = false)
@Component(base = true)
abstract class BulletComp implements Timedc, Damagec, Hitboxc, Teamc, Posc, Drawc, Shielderc, Ownerc, Velc, Bulletc, Timerc{
    @Import Team team;
    @Import Entityc owner;
    @Import float x, y, damage;

    IntSeq collided = new IntSeq(6);
    Object data;
    BulletType type;
    float fdata;
    transient boolean absorbed;

    @Override
    public void getCollisions(Cons<QuadTree> consumer){
        Seq<TeamData> data = state.teams.present;
        for(int i = 0; i < data.size; i++){
            if(data.items[i].team != team){
                consumer.get(data.items[i].tree());
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
        absorbed = true;
        remove();
    }

    @Replace
    public float clipSize(){
        return type.drawSize;
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

        if(other instanceof Healthc h){
            health = h.health();
            h.damage(damage);
        }

        if(other instanceof Unit unit){
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

        if(owner instanceof WallBuild && player != null && team == player.team() && other instanceof Unit unit && unit.dead){
            Events.fire(Trigger.phaseDeflectHit);
        }
    }

    @Override
    public void update(){
        type.update(self());

        if(type.collidesTiles && type.collides && type.collidesGround){
            world.raycastEach(World.toTile(lastX()), World.toTile(lastY()), tileX(), tileY(), (x, y) -> {

                Building tile = world.build(x, y);
                if(tile == null || !isAdded()) return false;

                if(tile.collide(self()) && type.testCollision(self(), tile) && !tile.dead() && (type.collidesTeam || tile.team != team) && !(type.pierceBuilding && collided.contains(tile.id))){
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

                    type.hitTile(self(), tile, health, true);

                    return !type.pierceBuilding;
                }

                return false;
            });
        }

        if(type.pierceCap != -1 && collided.size >= type.pierceCap){
            remove();
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
