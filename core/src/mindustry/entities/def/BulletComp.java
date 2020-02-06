package mindustry.entities.def;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

@Component
abstract class BulletComp implements Timedc, Damagec, Hitboxc, Teamc, Posc, Drawc, Shielderc, Ownerc, Velc, Bulletc, Timerc{
    private float lifeScl;

    Object data;
    BulletType type;
    float damage;

    @Override
    public void add(){
        type.init(this);

        drag(type.drag);
        hitSize(type.hitSize);
        lifetime(lifeScl * type.lifetime);
    }

    @Override
    public void remove(){
        type.despawned(this);
    }

    @Override
    public float getLifetime(){
        return type.lifetime;
    }

    @Override
    public float damageMultiplier(){
        if(owner() instanceof Unitc){
            return ((Unitc)owner()).damageMultiplier();
        }
        return 1f;
    }

    @Override
    public void absorb(){
        //TODO
        remove();
    }

    @Override
    public float clipSize(){
        return type.drawSize;
    }

    @Override
    public float damage(){
        return type.damage * damageMultiplier();
    }

    @Override
    public void collision(Hitboxc other, float x, float y){
        if(!type.pierce) remove();
        type.hit(this, x, y);

        if(other instanceof Unitc){
            Unitc unit = (Unitc)other;
            unit.vel().add(Tmp.v3.set(other.x(), other.y()).sub(x, y).setLength(type.knockback / unit.mass()));
            unit.apply(type.status, type.statusDuration);
        }
    }

    @Override
    public void update(){
        type.update(this);

        if(type.hitTiles){
            world.raycastEach(world.toTile(lastX()), world.toTile(lastY()), tileX(), tileY(), (x, y) -> {

                Tile tile = world.ltile(x, y);
                if(tile == null) return false;

                if(tile.entity != null && tile.entity.collide(this) && type.collides(this, tile) && !tile.entity.dead() && (type.collidesTeam || tile.team() != team())){
                    if(tile.team() != team()){
                        tile.entity.collision(this);
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
        type.draw(this);
        //TODO refactor
        renderer.lights.add(x(), y(), 16f, Pal.powerLight, 0.3f);
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
