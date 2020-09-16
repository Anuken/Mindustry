package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Utility class for damaging in an area. */
public class Damage{
    private static Tile furthest;
    private static Rect rect = new Rect();
    private static Rect hitrect = new Rect();
    private static Vec2 tr = new Vec2();
    private static GridBits bits = new GridBits(30, 30);
    private static IntQueue propagation = new IntQueue();
    private static IntSet collidedBlocks = new IntSet();
    private static Building tmpBuilding;
    private static Unit tmpUnit;

    /** Creates a dynamic explosion based on specified parameters. */
    public static void dynamicExplosion(float x, float y, float flammability, float explosiveness, float power, float radius, Color color, boolean damage){
        if(damage){
            for(int i = 0; i < Mathf.clamp(power / 20, 0, 6); i++){
                int branches = 5 + Mathf.clamp((int)(power / 30), 1, 20);
                Time.run(i * 2f + Mathf.random(4f), () -> Lightning.create(Team.derelict, Pal.power, 3, x, y, Mathf.random(360f), branches + Mathf.range(2)));
            }

            for(int i = 0; i < Mathf.clamp(flammability / 4, 0, 30); i++){
                Time.run(i / 2f, () -> Call.createBullet(Bullets.fireball, Team.derelict, x, y, Mathf.random(360f), Bullets.fireball.damage, 1, 1));
            }

            int waves = Mathf.clamp((int)(explosiveness / 4), 0, 30);

            for(int i = 0; i < waves; i++){
                int f = i;
                Time.run(i * 2f, () -> {
                    Damage.damage(x, y, Mathf.clamp(radius + explosiveness, 0, 50f) * ((f + 1f) / waves), explosiveness / 2f);
                    Fx.blockExplosionSmoke.at(x + Mathf.range(radius), y + Mathf.range(radius));
                });
            }
        }

        if(explosiveness > 15f){
            Fx.shockwave.at(x, y);
        }

        if(explosiveness > 30f){
            Fx.bigShockwave.at(x, y);
        }

        float shake = Math.min(explosiveness / 4f + 3f, 9f);
        Effect.shake(shake, shake, x, y);
        Fx.dynamicExplosion.at(x, y, radius / 8f);
    }

    public static void createIncend(float x, float y, float range, int amount){
        for(int i = 0; i < amount; i++){
            float cx = x + Mathf.range(range);
            float cy = y + Mathf.range(range);
            Tile tile = world.tileWorld(cx, cy);
            if(tile != null){
                Fires.create(tile);
            }
        }
    }

    /** Collides a bullet with blocks in a laser, taking into account absorption blocks. Resulting length is stored in the bullet's fdata. */
    public static float collideLaser(Bullet b, float length){
        Tmp.v1.trns(b.rotation(), length);

        furthest = null;

        world.raycast(b.tileX(), b.tileY(), world.toTile(b.x + Tmp.v1.x), world.toTile(b.y + Tmp.v1.y),
        (x, y) -> (furthest = world.tile(x, y)) != null && furthest.team() != b.team && furthest.block().absorbLasers);

        float resultLength = furthest != null ? Math.max(6f, b.dst(furthest.worldx(), furthest.worldy())) : length;

        Damage.collideLine(b, b.team, b.type.hitEffect, b.x, b.y, b.rotation(), resultLength);
        b.fdata = furthest != null ? resultLength : length;

        return resultLength;
    }

    public static void collideLine(Bullet hitter, Team team, Effect effect, float x, float y, float angle, float length){
        collideLine(hitter, team, effect, x, y, angle, length, false);
    }

    /**
     * Damages entities in a line.
     * Only enemies of the specified team are damaged.
     */
    public static void collideLine(Bullet hitter, Team team, Effect effect, float x, float y, float angle, float length, boolean large){
        collidedBlocks.clear();
        tr.trns(angle, length);
        Intc2 collider = (cx, cy) -> {
            Building tile = world.build(cx, cy);
            if(tile != null && !collidedBlocks.contains(tile.pos()) && tile.team != team && tile.collide(hitter)){
                tile.collision(hitter);
                collidedBlocks.add(tile.pos());
                hitter.type.hit(hitter, tile.x, tile.y);
            }
        };

        if(hitter.type.collidesGround){
            world.raycastEachWorld(x, y, x + tr.x, y + tr.y, (cx, cy) -> {
                collider.get(cx, cy);
                if(large){
                    for(Point2 p : Geometry.d4){
                        collider.get(cx + p.x, cy + p.y);
                    }
                }
                return false;
            });
        }

        rect.setPosition(x, y).setSize(tr.x, tr.y);
        float x2 = tr.x + x, y2 = tr.y + y;

        if(rect.width < 0){
            rect.x += rect.width;
            rect.width *= -1;
        }

        if(rect.height < 0){
            rect.y += rect.height;
            rect.height *= -1;
        }

        float expand = 3f;

        rect.y -= expand;
        rect.x -= expand;
        rect.width += expand * 2;
        rect.height += expand * 2;

        Cons<Unit> cons = e -> {
            if(!e.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround)) return;

            e.hitbox(hitrect);
            Rect other = hitrect;
            other.y -= expand;
            other.x -= expand;
            other.width += expand * 2;
            other.height += expand * 2;

            Vec2 vec = Geometry.raycastRect(x, y, x2, y2, other);

            if(vec != null){
                effect.at(vec.x, vec.y);
                e.collision(hitter, vec.x, vec.y);
                hitter.collision(e, vec.x, vec.y);
            }
        };

        Units.nearbyEnemies(team, rect, cons);
    }

    /**
     * Casts forward in a line.
     * @return the first encountered object.
     */
    public static Healthc linecast(Bullet hitter, float x, float y, float angle, float length){
        tr.trns(angle, length);

        if(hitter.type.collidesGround){
            tmpBuilding = null;

            world.raycastEachWorld(x, y, x + tr.x, y + tr.y, (cx, cy) -> {
                Building tile = world.build(cx, cy);
                if(tile != null && tile.team != hitter.team){
                    tmpBuilding = tile;
                    //TODO return tile
                    return true;
                }
                return false;
            });

            if(tmpBuilding != null) return tmpBuilding;
        }

        rect.setPosition(x, y).setSize(tr.x, tr.y);
        float x2 = tr.x + x, y2 = tr.y + y;

        if(rect.width < 0){
            rect.x += rect.width;
            rect.width *= -1;
        }

        if(rect.height < 0){
            rect.y += rect.height;
            rect.height *= -1;
        }

        float expand = 3f;

        rect.y -= expand;
        rect.x -= expand;
        rect.width += expand * 2;
        rect.height += expand * 2;

        tmpUnit = null;

        Cons<Unit> cons = e -> {
            if((tmpUnit != null && e.dst2(x, y) > tmpUnit.dst2(x, y)) || !e.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround)) return;

            e.hitbox(hitrect);
            Rect other = hitrect;
            other.y -= expand;
            other.x -= expand;
            other.width += expand * 2;
            other.height += expand * 2;

            Vec2 vec = Geometry.raycastRect(x, y, x2, y2, other);

            if(vec != null){
                tmpUnit = e;
            }
        };

        Units.nearbyEnemies(hitter.team, rect, cons);

        return tmpUnit;
    }

    /** Damages all entities and blocks in a radius that are enemies of the team. */
    public static void damageUnits(Team team, float x, float y, float size, float damage, Boolf<Unit> predicate, Cons<Unit> acceptor){
        Cons<Unit> cons = entity -> {
            if(!predicate.get(entity)) return;

            entity.hitbox(hitrect);
            if(!hitrect.overlaps(rect)){
                return;
            }
            entity.damage(damage);
            acceptor.get(entity);
        };

        rect.setSize(size * 2).setCenter(x, y);
        if(team != null){
            Units.nearbyEnemies(team, rect, cons);
        }else{
            Units.nearby(rect, cons);
        }
    }

    /** Damages everything in a radius. */
    public static void damage(float x, float y, float radius, float damage){
        damage(null, x, y, radius, damage, false);
    }

    /** Damages all entities and blocks in a radius that are enemies of the team. */
    public static void damage(Team team, float x, float y, float radius, float damage){
        damage(team, x, y, radius, damage, false);
    }

    /** Damages all entities and blocks in a radius that are enemies of the team. */
    public static void damage(Team team, float x, float y, float radius, float damage, boolean air, boolean ground){
        damage(team, x, y, radius, damage, false, air, ground);
    }

    /** Applies a status effect to all enemy units in a range. */
    public static void status(Team team, float x, float y, float radius, StatusEffect effect, float duration, boolean air, boolean ground){
        Cons<Unit> cons = entity -> {
            if(entity.team == team || !entity.within(x, y, radius) || (entity.isFlying() && !air) || (entity.isGrounded() && !ground)){
                return;
            }

            entity.apply(effect, duration);
        };

        rect.setSize(radius * 2).setCenter(x, y);
        if(team != null){
            Units.nearbyEnemies(team, rect, cons);
        }else{
            Units.nearby(rect, cons);
        }
    }

    /** Damages all entities and blocks in a radius that are enemies of the team. */
    public static void damage(Team team, float x, float y, float radius, float damage, boolean complete){
        damage(team, x, y, radius, damage, complete, true, true);
    }

    /** Damages all entities and blocks in a radius that are enemies of the team. */
    public static void damage(Team team, float x, float y, float radius, float damage, boolean complete, boolean air, boolean ground){
        Cons<Unit> cons = entity -> {
            if(entity.team == team || !entity.within(x, y, radius) || (entity.isFlying() && !air) || (entity.isGrounded() && !ground)){
                return;
            }
            float amount = calculateDamage(x, y, entity.getX(), entity.getY(), radius, damage);
            entity.damage(amount);
            //TODO better velocity displacement
            float dst = tr.set(entity.getX() - x, entity.getY() - y).len();
            entity.vel.add(tr.setLength((1f - dst / radius) * 2f / entity.mass()));

            if(complete && damage >= 9999999f && entity.isPlayer()){
                Events.fire(Trigger.exclusionDeath);
            }
        };

        rect.setSize(radius * 2).setCenter(x, y);
        if(team != null){
            Units.nearbyEnemies(team, rect, cons);
        }else{
            Units.nearby(rect, cons);
        }

        if(ground){
            if(!complete){
                int trad = (int)(radius / tilesize);
                Tile tile = world.tileWorld(x, y);
                if(tile != null){
                    tileDamage(team, tile.x, tile.y, trad, damage);
                }
            }else{
                completeDamage(team, x, y, radius, damage);
            }
        }
    }

    public static void tileDamage(Team team, int startx, int starty, int baseRadius, float baseDamage){
        //tile damage is posted, so that destroying a block that causes a chain explosion will run in the next frame
        //this prevents recursive damage calls from messing up temporary variables
        Core.app.post(() -> {

            bits.clear();
            propagation.clear();
            int bitOffset = bits.width() / 2;

            propagation.addFirst(PropCell.get((byte)0, (byte)0, (short)baseDamage));
            //clamp radius to fit bits
            int radius = Math.min(baseRadius, bits.width() / 2);

            while(!propagation.isEmpty()){
                int prop = propagation.removeLast();
                int x = PropCell.x(prop);
                int y = PropCell.y(prop);
                int damage = PropCell.damage(prop);
                //manhattan distance used for calculating falloff, results in a diamond pattern
                int dst = Math.abs(x) + Math.abs(y);

                int scaledDamage = (int)(damage * (1f - (float)dst / radius));

                bits.set(bitOffset + x, bitOffset + y);
                Tile tile = world.tile(startx + x, starty + y);

                if(scaledDamage <= 0 || tile == null) continue;

                //apply damage to entity if needed
                if(tile.build != null && tile.team() != team){
                    int health = (int)tile.build.health();
                    if(tile.build.health() > 0){
                        tile.build.damage(scaledDamage);
                        scaledDamage -= health;

                        if(scaledDamage <= 0) continue;
                    }
                }

                for(Point2 p : Geometry.d4){
                    if(!bits.get(bitOffset + x + p.x, bitOffset + y + p.y)){
                        propagation.addFirst(PropCell.get((byte)(x + p.x), (byte)(y + p.y), (short)scaledDamage));
                    }
                }
            }
        });

    }

    private static void completeDamage(Team team, float x, float y, float radius, float damage){
        int trad = (int)(radius / tilesize);
        for(int dx = -trad; dx <= trad; dx++){
            for(int dy = -trad; dy <= trad; dy++){
                Tile tile = world.tile(Math.round(x / tilesize) + dx, Math.round(y / tilesize) + dy);
                if(tile != null && tile.build != null && (team == null ||team.isEnemy(tile.team())) && Mathf.dst(dx, dy) <= trad){
                    tile.build.damage(damage);
                }
            }
        }
    }

    private static float calculateDamage(float x, float y, float tx, float ty, float radius, float damage){
        float dist = Mathf.dst(x, y, tx, ty);
        float falloff = 0.4f;
        float scaled = Mathf.lerp(1f - dist / radius, 1f, falloff);
        return damage * scaled;
    }

    @Struct
    static
    class PropCellStruct{
        byte x;
        byte y;
        short damage;
    }
}
