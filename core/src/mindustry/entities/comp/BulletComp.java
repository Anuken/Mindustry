package mindustry.entities.comp;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

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
            tileRaycast(World.toTile(lastX()), World.toTile(lastY()), tileX(), tileY());
        }

        if(type.pierceCap != -1 && collided.size >= type.pierceCap){
            hit = true;
            remove();
        }
    }

    //copy-paste of World#raycastEach, inlined for lambda capture performance.
    @Override
    public void tileRaycast(int x0f, int y0f, int x1, int y1){
        int x = x0f, dx = Math.abs(x1 - x), sx = x < x1 ? 1 : -1;
        int y = y0f, dy = Math.abs(y1 - y), sy = y < y1 ? 1 : -1;
        int e2, err = dx - dy;
        int ww = world.width(), wh = world.height();

        while(x >= 0 && y >= 0 && x < ww && y < wh){
            Building build = world.build(x, y);

            if(type.collideFloor || type.collideTerrain){
                Tile tile = world.tile(x, y);
                if(
                    type.collideFloor && (tile == null || tile.floor().hasSurface() || tile.block() != Blocks.air) ||
                    type.collideTerrain && tile != null && tile.block() instanceof StaticWall
                ){
                    type.despawned(self());
                    remove();
                    hit = true;
                    return;
                }
            }

            if(build != null && isAdded() && build.collide(self()) && type.testCollision(self(), build)
                && !build.dead() && (type.collidesTeam || build.team != team) && !(type.pierceBuilding && hasCollided(build.id))){

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

                //stop raycasting when building is hit
                if(type.pierceBuilding) return;
            }

            if(x == x1 && y == y1) break;

            e2 = 2 * err;
            if(e2 > -dy){
                err -= dy;
                x += sx;
            }

            if(e2 < dx){
                err += dx;
                y += sy;
            }
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
