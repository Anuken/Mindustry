package mindustry.entities.comp;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
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
abstract class BulletComp implements Timedc, Damagec, Hitboxc, Teamc, Posc, Drawc, Shielderc, Ownerc, Bulletc, Timerc{
    @Import Team team;
    @Import Entityc owner;
    @Import float x, y, damage, lastX, lastY, time, lifetime;

    IntSeq collided = new IntSeq(6);
    BulletType type;
    Vec2 vel = new Vec2();

    Object data;
    float fdata;

    @ReadOnly
    private float rotation;

    //setting this variable to true prevents lifetime from decreasing for a frame.
    transient boolean keepAlive;
    transient boolean justSpawned = true;
    /** Unlike the owner, the shooter is the original entity that created this bullet. For a second-stage missile, the shooter would be the turret, but the owner would be the last missile stage.*/
    transient Entityc shooter;
    transient @Nullable Tile aimTile;
    transient float aimX, aimY;
    transient float originX, originY;
    transient @Nullable Mover mover;
    transient boolean absorbed, hit;
    transient @Nullable Trail trail;
    transient int frags;

    transient Posc stickyTarget;
    transient float stickyX, stickyY, stickyRotation, stickyRotationOffset;

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
        if(Groups.isClearing) return;

        //'despawned' only counts when the bullet is killed externally or reaches the end of life
        if(!hit){
            type.despawned(self());
        }
        type.removed(self());
        collided.clear();
    }

    @Override
    public float damageMultiplier(){
        return type.damageMultiplier(self());
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
            && !(other instanceof Unit f && !f.checkTarget(type.collidesAir, type.collidesGround))
            && !(type.pierce && hasCollided(other.id())) && stickyTarget == null; //prevent multiple collisions
    }

    @MethodPriority(100)
    @Override
    public void collision(Hitboxc other, float x, float y){
        if(type.sticky){
            if(stickyTarget == null){
                //tunnel into the target a bit for better visuals
                this.x = x + vel.x;
                this.y = y + vel.y;
                stickTo(other);
            }
        }else{
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
    }

    public void stickTo(Posc other){
        lifetime += type.stickyExtraLifetime;
        //sticky bullets don't actually hit anything.
        stickyX = this.x - other.x();
        stickyY = this.y - other.y();
        stickyTarget = other;
        stickyRotationOffset = rotation;
        stickyRotation = (other instanceof Rotc rot ? rot.rotation() : 0f);
    }

    @Override
    public void update(){
        //for one frame, bullets do not move - this is because bullet.update() is called immediately after the weapon updates
        //if the bullet moved immediately, it would spawn visually offset from the weapon at low FPS values
        if(!justSpawned){
            x += vel.x * Time.delta;
            y += vel.y * Time.delta;
            vel.scl(Math.max(1f - type.drag * Time.delta, 0));
        }
        justSpawned = false;

        if(mover != null){
            mover.move(self());
        }

        if(type.accel != 0){
            vel.setLength(vel.len() + type.accel * Time.delta);
        }

        type.update(self());

        if(stickyTarget != null){
            //only stick to things that still exist in the world
            if(stickyTarget instanceof Healthc h && h.isValid()){
                float rotate = (stickyTarget instanceof Rotc rot ? rot.rotation() - stickyRotation : 0f);
                set(Tmp.v1.set(stickyX, stickyY).rotate(rotate).add(stickyTarget));
                this.rotation = rotate + stickyRotationOffset;
                vel.setAngle(this.rotation);
            }
        }else if(type.collidesTiles && type.collides && type.collidesGround){
            tileRaycast(World.toTile(lastX), World.toTile(lastY), tileX(), tileY());
        }

        if(type.removeAfterPierce && type.pierceCap != -1 && collided.size >= type.pierceCap){
            hit = true;
            remove();
        }

        if(keepAlive){
            time -= Time.delta;
            keepAlive = false;
        }
    }

    public void moveRelative(float x, float y){
        float rot = rotation();
        this.x += Angles.trnsx(rot, x * Time.delta, y * Time.delta);
        this.y += Angles.trnsy(rot, x * Time.delta, y * Time.delta);
    }

    public void turn(float x, float y){
        float ang = vel.angle();
        vel.add(Angles.trnsx(ang, x * Time.delta, y * Time.delta), Angles.trnsy(ang, x * Time.delta, y * Time.delta)).limit(type.speed);
    }

    public boolean checkUnderBuild(Building build, float x, float y){
        return
            (!build.block.underBullets ||
            //direct hit on correct tile
            (aimTile != null && aimTile.build == build) ||
            //bullet type allows hitting under bullets
            type.hitUnder ||
            //same team has no 'under build' mechanics
            (build.team == team) ||
            //a piercing bullet overshot the aim tile, it's fine to hit things now
            (type.pierce && aimTile != null && Mathf.dst(x, y, originX, originY) > aimTile.dst(originX, originY) + 2f) ||
            //there was nothing to aim at
            (aimX == -1f && aimY == -1f));
    }

    //copy-paste of World#raycastEach, inlined for lambda capture performance.
    @Override
    public void tileRaycast(int x1, int y1, int x2, int y2){
        int x = x1, dx = Math.abs(x2 - x), sx = x < x2 ? 1 : -1;
        int y = y1, dy = Math.abs(y2 - y), sy = y < y2 ? 1 : -1;
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
                    remove();
                    hit = true;
                    return;
                }
            }

            if(build != null && isAdded()
                && checkUnderBuild(build, x * tilesize, y * tilesize)
                && build.collide(self()) && type.testCollision(self(), build)
                && !build.dead() && (type.collidesTeam || build.team != team) && !(type.pierceBuilding && hasCollided(build.id))){

                if(type.sticky){
                    if(build.team != team){
                        //stick to edge of block
                        Vec2 hit = Geometry.raycastRect(lastX, lastY, x, y, Tmp.r1.setCentered(x * tilesize, y * tilesize, tilesize, tilesize));
                        if(hit != null){
                            this.x = hit.x;
                            this.y = hit.y;
                        }

                        stickTo(build);

                        return;
                    }
                }else{
                    boolean remove = false;
                    float health = build.health;

                    if(build.team != team){
                        remove = build.collision(self());
                    }

                    if(remove || type.collidesTeam){
                        if(Mathf.dst2(lastX, lastY, x * tilesize, y * tilesize) < Mathf.dst2(lastX, lastY, this.x, this.y)){
                            this.x = x * tilesize;
                            this.y = y * tilesize;
                        }

                        if(!type.pierceBuilding){
                            hit = true;
                            remove();
                        }else{
                            collided.add(build.id);
                        }
                    }

                    type.hitTile(self(), build, x * tilesize, y * tilesize, health, true);

                    //stop raycasting when building is hit
                    if(type.pierceBuilding) return;
                }
            }

            if(x == x2 && y == y2) break;

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

        if(type.underwater){
            Drawf.underwater(() -> type.draw(self()));
        }else{
            type.draw(self());
        }
        type.drawLight(self());

        Draw.reset();
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
