package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
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
    private static Vec2 tr = new Vec2(), seg1 = new Vec2(), seg2 = new Vec2();
    private static Seq<Unit> units = new Seq<>();
    private static IntSet collidedBlocks = new IntSet();
    private static Building tmpBuilding;
    private static Unit tmpUnit;
    private static IntFloatMap damages = new IntFloatMap();

    /** Creates a dynamic explosion based on specified parameters. */
    public static void dynamicExplosion(float x, float y, float flammability, float explosiveness, float power, float radius, boolean damage){
        dynamicExplosion(x, y, flammability, explosiveness, power, radius, damage, true, null, Fx.dynamicExplosion);
    }

    /** Creates a dynamic explosion based on specified parameters. */
    public static void dynamicExplosion(float x, float y, float flammability, float explosiveness, float power, float radius, boolean damage, Effect explosionFx){
        dynamicExplosion(x, y, flammability, explosiveness, power, radius, damage, true, null, explosionFx);
    }

    /** Creates a dynamic explosion based on specified parameters. */
    public static void dynamicExplosion(float x, float y, float flammability, float explosiveness, float power, float radius, boolean damage, boolean fire, @Nullable Team ignoreTeam){
        dynamicExplosion(x, y, flammability, explosiveness, power, radius, damage, fire, ignoreTeam, Fx.dynamicExplosion);
    }

    /** Creates a dynamic explosion based on specified parameters. */
    public static void dynamicExplosion(float x, float y, float flammability, float explosiveness, float power, float radius, boolean damage, boolean fire, @Nullable Team ignoreTeam, Effect explosionFx){
        if(damage){
            for(int i = 0; i < Mathf.clamp(power / 700, 0, 8); i++){
                int length = 5 + Mathf.clamp((int)(power / 500), 1, 20);
                Time.run(i * 0.8f + Mathf.random(4f), () -> Lightning.create(Team.derelict, Pal.power, 3, x, y, Mathf.random(360f), length + Mathf.range(2)));
            }

            if(fire){
                for(int i = 0; i < Mathf.clamp(flammability / 4, 0, 30); i++){
                    Time.run(i / 2f, () -> Call.createBullet(Bullets.fireball, Team.derelict, x, y, Mathf.random(360f), Bullets.fireball.damage, 1, 1));
                }
            }

            int waves = explosiveness <= 2 ? 0 : Mathf.clamp((int)(explosiveness / 11), 1, 25);

            for(int i = 0; i < waves; i++){
                int f = i;
                Time.run(i * 2f, () -> {
                    damage(ignoreTeam, x, y, Mathf.clamp(radius + explosiveness, 0, 50f) * ((f + 1f) / waves), explosiveness / 2f, false);
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
        explosionFx.at(x, y, radius / 8f);
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

    public static @Nullable Building findAbsorber(Team team, float x1, float y1, float x2, float y2){
        tmpBuilding = null;

        boolean found = world.raycast(World.toTile(x1), World.toTile(y1), World.toTile(x2), World.toTile(y2),
        (x, y) -> (tmpBuilding = world.build(x, y)) != null && tmpBuilding.team != team && tmpBuilding.block.absorbLasers);

        return found ? tmpBuilding : null;
    }

    public static float findLaserLength(Bullet b, float length){
        Tmp.v1.trnsExact(b.rotation(), length);

        furthest = null;

        boolean found = world.raycast(b.tileX(), b.tileY(), World.toTile(b.x + Tmp.v1.x), World.toTile(b.y + Tmp.v1.y),
        (x, y) -> (furthest = world.tile(x, y)) != null && furthest.team() != b.team && furthest.block().absorbLasers);

        return found && furthest != null ? Math.max(6f, b.dst(furthest.worldx(), furthest.worldy())) : length;
    }

    /** Collides a bullet with blocks in a laser, taking into account absorption blocks. Resulting length is stored in the bullet's fdata. */
    public static float collideLaser(Bullet b, float length, boolean large){
        float resultLength = findLaserLength(b, length);

        collideLine(b, b.team, b.type.hitEffect, b.x, b.y, b.rotation(), resultLength, large);

        b.fdata = resultLength;

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
        collideLine(hitter, team, effect, x, y, angle, length, large, true);
    }

    /**
     * Damages entities in a line.
     * Only enemies of the specified team are damaged.
     */
    public static void collideLine(Bullet hitter, Team team, Effect effect, float x, float y, float angle, float length, boolean large, boolean laser){
        if(laser) length = findLaserLength(hitter, length);

        collidedBlocks.clear();
        tr.trnsExact(angle, length);

        Intc2 collider = (cx, cy) -> {
            Building tile = world.build(cx, cy);
            boolean collide = tile != null && collidedBlocks.add(tile.pos());

            if(hitter.damage > 0){
                float health = !collide ? 0 : tile.health;

                if(collide && tile.team != team && tile.collide(hitter)){
                    tile.collision(hitter);
                    hitter.type.hit(hitter, tile.x, tile.y);
                }

                //try to heal the tile
                if(collide && hitter.type.testCollision(hitter, tile)){
                    hitter.type.hitTile(hitter, tile, health, false);
                }
            }
        };

        if(hitter.type.collidesGround){
            seg1.set(x, y);
            seg2.set(seg1).add(tr);
            world.raycastEachWorld(x, y, seg2.x, seg2.y, (cx, cy) -> {
                collider.get(cx, cy);

                for(Point2 p : Geometry.d4){
                    Tile other = world.tile(p.x + cx, p.y + cy);
                    if(other != null && (large || Intersector.intersectSegmentRectangle(seg1, seg2, other.getBounds(Tmp.r1)))){
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
            e.hitbox(hitrect);

            Vec2 vec = Geometry.raycastRect(x, y, x2, y2, hitrect.grow(expand * 2));

            if(vec != null && hitter.damage > 0){
                effect.at(vec.x, vec.y);
                e.collision(hitter, vec.x, vec.y);
                hitter.collision(e, vec.x, vec.y);
            }
        };

        units.clear();

        Units.nearbyEnemies(team, rect, u -> {
            if(u.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround)){
                units.add(u);
            }
        });

        units.sort(u -> u.dst2(hitter));
        units.each(cons);
    }

    /**
     * Casts forward in a line.
     * @return the first encountered object.
     */
    public static Healthc linecast(Bullet hitter, float x, float y, float angle, float length){
        tr.trns(angle, length);
        
        tmpBuilding = null;

        if(hitter.type.collidesGround){
            world.raycastEachWorld(x, y, x + tr.x, y + tr.y, (cx, cy) -> {
                Building tile = world.build(cx, cy);
                if(tile != null && tile.team != hitter.team){
                    tmpBuilding = tile;
                    return true;
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

        tmpUnit = null;

        Units.nearbyEnemies(hitter.team, rect, e -> {
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
        });

        if(tmpBuilding != null && tmpUnit != null){
            if(Mathf.dst2(x, y, tmpBuilding.getX(), tmpBuilding.getY()) <= Mathf.dst2(x, y, tmpUnit.getX(), tmpUnit.getY())){
                return tmpBuilding;
            }
        }else if(tmpBuilding != null){
            return tmpBuilding;
        }

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
                tileDamage(team, World.toTile(x), World.toTile(y), radius / tilesize, damage);
            }else{
                completeDamage(team, x, y, radius, damage);
            }
        }
    }

    public static void tileDamage(Team team, int x, int y, float baseRadius, float damage){

        Core.app.post(() -> {

            var in = world.build(x, y);
            //spawned inside a multiblock. this means that damage needs to be dealt directly.
            //why? because otherwise the building would absorb everything in one cell, which means much less damage than a nearby explosion.
            //this needs to be compensated
            if(in != null && in.team != team && in.block.size > 1 && in.health > damage){
                //deal the damage of an entire side, to be equivalent with maximum 'standard' damage
                in.damage(team, damage * Math.min((in.block.size), baseRadius * 0.4f));
                //no need to continue with the explosion
                return;
            }

            //cap radius to prevent lag
            float radius = Math.min(baseRadius, 30), rad2 = radius * radius;
            int rays = Mathf.ceil(radius * 2 * Mathf.pi);
            double spacing = Math.PI * 2.0 / rays;
            damages.clear();

            //raycast from each angle
            for(int i = 0; i <= rays; i++){
                float dealt = 0f;
                int startX = x;
                int startY = y;
                int endX = x + (int)(Math.cos(spacing * i) * radius), endY = y + (int)(Math.sin(spacing * i) * radius);

                int xDist = Math.abs(endX - startX);
                int yDist = -Math.abs(endY - startY);
                int xStep = (startX < endX ? +1 : -1);
                int yStep = (startY < endY ? +1 : -1);
                int error = xDist + yDist;

                while(startX != endX || startY != endY){
                    var build = world.build(startX, startY);
                    if(build != null && build.team != team){
                        //damage dealt at circle edge
                        float edgeScale = 0.6f;
                        float mult = (1f-(Mathf.dst2(startX, startY, x, y) / rad2) + edgeScale) / (1f + edgeScale);
                        float next = damage * mult - dealt;
                        //register damage dealt
                        int p = Point2.pack(startX, startY);
                        damages.put(p, Math.max(damages.get(p), next));
                        //register as hit
                        dealt += build.health;

                        if(next - dealt <= 0){
                            break;
                        }
                    }

                    if(2 * error - yDist > xDist - 2 * error){
                        error += yDist;
                        startX += xStep;
                    }else{
                        error += xDist;
                        startY += yStep;
                    }
                }
            }

            //apply damage
            for(var e : damages){
                int cx = Point2.x(e.key), cy = Point2.y(e.key);
                var build = world.build(cx, cy);
                if(build != null){
                    build.damage(team, e.value);
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
}
