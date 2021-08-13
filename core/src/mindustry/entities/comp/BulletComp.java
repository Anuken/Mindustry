package mindustry.entities.comp;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

@EntityDef(value = {Bulletc.class}, pooled = true, serialize = false)
@Component(base = true)
abstract class BulletComp implements Timedc, Damagec, Hitboxc, Teamc, Posc, Drawc, Shielderc, Ownerc, Velc, Bulletc, Timerc{
    @Import Team team;
    @Import Entityc owner;
    @Import float x, y, damage;
    @Import Vec2 vel;

    IntSeq collided = new IntSeq(6);
    Object data;
    BulletType type;
    float fdata;

    @ReadOnly
    private float rotation;

    transient boolean absorbed, hit;
    transient @Nullable Trail trail;

    @Override
    public void getCollisions(Cons<QuadTree> consumer){
        Seq<TeamData> data = state.teams.present;
        for(int i = 0; i < data.size; i++){
            if(data.items[i].team != team){
                consumer.get(data.items[i].tree());
            }
        }
    }

    //bullets always considered local
    @Override
    @Replace
    public boolean isLocal(){
        return true;
    }

    @Override
    public void add(){
        type.init(self());
    }

    @Override
    public void remove(){
        //'despawned' only counts when the bullet is killed externally or reaches the end of life
        if(!hit){
            type.despawned(self());
        }
        type.removed(self());
        collided.clear();
    }

    @Override
    public float damageMultiplier(){
        if(owner instanceof Unit u) return u.damageMultiplier() * state.rules.unitDamage(team);
        if(owner instanceof Building) return state.rules.blockDamage(team);

        return 1f;
    }

    @Override
    public void absorb(){
        absorbed = true;
        remove();
    }

    public boolean hasCollided(int id){
        return collided.size != 0 && collided.contains(id);
    }

    @Replace
    public float clipSize(){
        return type.drawSize;
    }

    @Replace
    @Override
    public boolean collides(Hitboxc other){
        return type.collides && (other instanceof Teamc t && t.team() != team)
            && !(other instanceof Flyingc f && !f.checkTarget(type.collidesAir, type.collidesGround))
            && !(type.pierce && hasCollided(other.id())); //prevent multiple collisions
    }

    @MethodPriority(100)
    @Override
    public void collision(Hitboxc other, float x, float y){
        type.hit(self(), x, y);

        //must be last.
        if(!type.pierce){
            hit = true;
            remove();
        }else{
            collided.add(other.id());
        }

        type.hitEntity(self(), other, other instanceof Healthc h ? h.health() : 0f);
    }

    @Override
    public void update(){
        type.update(self());

        if(type.collidesTiles && type.collides && type.collidesGround){
            world.raycastEach(World.toTile(lastX()), World.toTile(lastY()), tileX(), tileY(), (x, y) -> {

                Building build = world.build(x, y);
                if(build == null || !isAdded()) return false;

                if(build.collide(self()) && type.testCollision(self(), build) && !build.dead() && (type.collidesTeam || build.team != team) && !(type.pierceBuilding && hasCollided(build.id))){
                    boolean remove = false;

                    float health = build.health;

                    if(build.team != team){
                        remove = build.collision(self());
                    }

                    if(remove || type.collidesTeam){
                        if(!type.pierceBuilding){
                            hit = true;
                            remove();
                        }else{
                            collided.add(build.id);
                        }
                    }

                    type.hitTile(self(), build, health, true);

                    return !type.pierceBuilding;
                }

                return false;
            });
        }

        if(type.pierceCap != -1 && collided.size >= type.pierceCap){
            hit = true;
            remove();
        }
    }

    @Override
    public void draw(){
        Draw.z(type.layer);

        type.draw(self());
        type.drawLight(self());
    }

    public void initVel(float angle, float amount){
        vel.trns(angle, amount);
        rotation = angle;
    }

    /** Sets the bullet's rotation in degrees. */
    @Override
    public void rotation(float angle){
        vel.setAngle(rotation = angle);
    }

    /** @return the bullet's rotation. */
    @Override
    public float rotation(){
        return vel.isZero(0.001f) ? rotation : vel.angle();
    }
}
