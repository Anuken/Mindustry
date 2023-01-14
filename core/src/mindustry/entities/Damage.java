package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
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
    private static final UnitDamageEvent bulletDamageEvent = new UnitDamageEvent();
    private static final Rect rect = new Rect();
    private static final Rect hitrect = new Rect();
    private static final Vec2 vec = new Vec2(), seg1 = new Vec2(), seg2 = new Vec2();
    private static final Seq<Unit> units = new Seq<>();
    private static final IntSet collidedBlocks = new IntSet();
    private static final IntFloatMap damages = new IntFloatMap();
    private static final Seq<Collided> collided = new Seq<>();
    private static final Pool<Collided> collidePool = Pools.get(Collided.class, Collided::new);
    private static final Seq<Building> builds = new Seq<>();
    private static final FloatSeq distances = new FloatSeq();

    private static Tile furthest;
    private static float maxDst = 0f;
    private static Building tmpBuilding;
    private static Unit tmpUnit;

    public static void applySuppression(Team team, float x, float y, float range, float reload, float maxDelay, float applyParticleChance, @Nullable Position source){
        builds.clear();
        indexer.eachBlock(null, x, y, range, build -> build.team != team, build -> {
            float prev = build.healSuppressionTime;
            build.applyHealSuppression(reload + 1f);

            //TODO maybe should be block field instead of instanceof check
            if(build.wasRecentlyHealed(60f * 12f) || build.block.suppressable){

                //add prev check so ability spam doesn't lead to particle spam (essentially, recently suppressed blocks don't get new particles)
                if(!headless && prev - Time.time <= reload/2f){
                    builds.add(build);
                }
            }
        });

        //to prevent particle spam, the amount of particles is to remain constant (scales with number of buildings)
        float scaledChance = applyParticleChance / builds.size;
        for(var build : builds){
            if(Mathf.chance(scaledChance)){
                Time.run(Mathf.random(maxDelay), () -> {
                    Fx.regenSuppressSeek.at(build.x + Mathf.range(build.block.size * tilesize / 2f), build.y + Mathf.range(build.block.size * tilesize / 2f), 0f, source);
                });
            }
        }
    }

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
                int length = 5 + Mathf.clamp((int)(Mathf.pow(power, 0.98f) / 500), 1, 18);
                Time.run(i * 0.8f + Mathf.random(4f), () -> Lightning.create(Team.derelict, Pal.power, 3 + Mathf.pow(power, 0.35f), x, y, Mathf.random(360f), length + Mathf.range(2)));
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

        boolean found = World.raycast(World.toTile(x1), World.toTile(y1), World.toTile(x2), World.toTile(y2),
        (x, y) -> (tmpBuilding = world.build(x, y)) != null && tmpBuilding.team != team && tmpBuilding.block.absorbLasers);

        return found ? tmpBuilding : null;
    }

    public static float findLaserLength(Bullet b, float length){
        vec.trnsExact(b.rotation(), length);

        furthest = null;

        boolean found = World.raycast(b.tileX(), b.tileY(), World.toTile(b.x + vec.x), World.toTile(b.y + vec.y),
        (x, y) -> (furthest = world.tile(x, y)) != null && furthest.team() != b.team && (furthest.build != null && furthest.build.absorbLasers()));

        return found && furthest != null ? Math.max(6f, b.dst(furthest.worldx(), furthest.worldy())) : length;
    }

    public static float findPierceLength(Bullet b, int pierceCap, float length){
        vec.trnsExact(b.rotation(), length);
        rect.setPosition(b.x, b.y).setSize(vec.x, vec.y).normalize().grow(3f);

        maxDst = Float.POSITIVE_INFINITY;

        distances.clear();

        World.raycast(b.tileX(), b.tileY(), World.toTile(b.x + vec.x), World.toTile(b.y + vec.y), (x, y) -> {
            //add distance to list so it can be processed
            var build = world.build(x, y);

            if(build != null && build.team != b.team && b.checkUnderBuild(build, x * tilesize, y * tilesize)){
                distances.add(b.dst(build));

                if(b.type.laserAbsorb && build.absorbLasers()){
                    maxDst = Math.min(maxDst, b.dst(build));
                    return true;
                }
            }

            return false;
        });

        Units.nearbyEnemies(b.team, rect, u -> {
            u.hitbox(hitrect);

            if(u.checkTarget(b.type.collidesAir, b.type.collidesGround) && u.hittable() && Intersector.intersectSegmentRectangle(b.x, b.y, b.x + vec.x, b.y + vec.y, hitrect)){
                distances.add(u.dst(b));
            }
        });

        distances.sort();

        //return either the length when not enough things were pierced,
        //or the last pierced object if there were enough blockages
        return Math.min(distances.size < pierceCap || pierceCap < 0 ? length : Math.max(6f, distances.get(pierceCap - 1)), maxDst);
    }

    /** Collides a bullet with blocks in a laser, taking into account absorption blocks. Resulting length is stored in the bullet's fdata. */
    public static float collideLaser(Bullet b, float length, boolean large, boolean laser, int pierceCap){
        float resultLength = findPierceLength(b, pierceCap, length);

        collideLine(b, b.team, b.type.hitEffect, b.x, b.y, b.rotation(), resultLength, large, laser, pierceCap);

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
        collideLine(hitter, team, effect, x, y, angle, length, large, laser, -1);
    }

    /**
     * Damages entities in a line.
     * Only enemies of the specified team are damaged.
     */
    public static void collideLine(Bullet hitter, Team team, Effect effect, float x, float y, float angle, float length, boolean large, boolean laser, int pierceCap){
        if(laser){
            length = findLaserLength(hitter, length);
        }else if(pierceCap > 0){
            length = findPierceLength(hitter, pierceCap, length);
        }

        collidedBlocks.clear();
        vec.trnsExact(angle, length);

        if(hitter.type.collidesGround){
            seg1.set(x, y);
            seg2.set(seg1).add(vec);
            World.raycastEachWorld(x, y, seg2.x, seg2.y, (cx, cy) -> {
                Building tile = world.build(cx, cy);
                boolean collide = tile != null && hitter.checkUnderBuild(tile, cx * tilesize, cy * tilesize)
                    && ((tile.team != team && tile.collide(hitter)) || hitter.type.testCollision(hitter, tile)) && collidedBlocks.add(tile.pos());
                if(collide){
                    collided.add(collidePool.obtain().set(cx * tilesize, cy * tilesize, tile));

                    for(Point2 p : Geometry.d4){
                        Tile other = world.tile(p.x + cx, p.y + cy);
                        if(other != null && (large || Intersector.intersectSegmentRectangle(seg1, seg2, other.getBounds(Tmp.r1)))){
                            Building build = other.build;
                            if(build != null && hitter.checkUnderBuild(build, cx * tilesize, cy * tilesize) && collidedBlocks.add(build.pos())){
                                collided.add(collidePool.obtain().set((p.x + cx * tilesize), (p.y + cy) * tilesize, build));
                            }
                        }
                    }
                }
                return false;
            });
        }

        float expand = 3f;

        rect.setPosition(x, y).setSize(vec.x, vec.y).normalize().grow(expand * 2f);
        float x2 = vec.x + x, y2 = vec.y + y;

        Units.nearbyEnemies(team, rect, u -> {
            if(u.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround) && u.hittable()){
                u.hitbox(hitrect);

                Vec2 vec = Geometry.raycastRect(x, y, x2, y2, hitrect.grow(expand * 2));

                if(vec != null){
                    collided.add(collidePool.obtain().set(vec.x, vec.y, u));
                }
            }
        });

        int[] collideCount = {0};
        collided.sort(c -> hitter.dst2(c.x, c.y));
        collided.each(c -> {
            if(hitter.damage > 0 && (pierceCap <= 0 || collideCount[0] < pierceCap)){
                if(c.target instanceof Unit u){
                    effect.at(c.x, c.y);
                    u.collision(hitter, c.x, c.y);
                    hitter.collision(u, c.x, c.y);
                    collideCount[0]++;
                }else if(c.target instanceof Building tile){
                    float health = tile.health;

                    if(tile.team != team && tile.collide(hitter)){
                        tile.collision(hitter);
                        hitter.type.hit(hitter, c.x, c.y);
                        collideCount[0]++;
                    }

                    //try to heal the tile
                    if(hitter.type.testCollision(hitter, tile)){
                        hitter.type.hitTile(hitter, tile, c.x, c.y, health, false);
                    }
                }
            }
        });

        collidePool.freeAll(collided);
        collided.clear();
    }

    /**
     * Damages entities on a point.
     * Only enemies of the specified team are damaged.
     */
    public static void collidePoint(Bullet hitter, Team team, Effect effect, float x, float y){

        if(hitter.type.collidesGround){
            Building build = world.build(World.toTile(x), World.toTile(y));

            if(build != null && hitter.damage > 0){
                float health = build.health;

                if(build.team != team && build.collide(hitter)){
                    build.collision(hitter);
                    hitter.type.hit(hitter, x, y);
                }

                //try to heal the tile
                if(hitter.type.testCollision(hitter, build)){
                    hitter.type.hitTile(hitter, build, x, y, health, false);
                }
            }
        }

        Units.nearbyEnemies(team, rect.setCentered(x, y, 1f), u -> {
            if(u.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround) && u.hittable()){
                effect.at(x, y);
                u.collision(hitter, x, y);
                hitter.collision(u, x, y);
            }
        });
    }

    /**
     * Casts forward in a line.
     * @return the first encountered object.
     */
    public static Healthc linecast(Bullet hitter, float x, float y, float angle, float length){
        vec.trns(angle, length);
        
        tmpBuilding = null;

        if(hitter.type.collidesGround){
            World.raycastEachWorld(x, y, x + vec.x, y + vec.y, (cx, cy) -> {
                Building tile = world.build(cx, cy);
                if(tile != null && tile.team != hitter.team){
                    tmpBuilding = tile;
                    return true;
                }
                return false;
            });
        }

        rect.setPosition(x, y).setSize(vec.x, vec.y);
        float x2 = vec.x + x, y2 = vec.y + y;

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
            if((tmpUnit != null && e.dst2(x, y) > tmpUnit.dst2(x, y)) || !e.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround) || !e.targetable(hitter.team)) return;

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
            if(!predicate.get(entity) || !entity.hittable()) return;

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
            if(entity.team == team || !entity.checkTarget(air, ground) || !entity.hittable() || !entity.within(x, y, radius)){
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
        damage(team, x, y, radius, damage, complete, air, ground, false, null);
    }

    /** Damages all entities and blocks in a radius that are enemies of the team. */
    public static void damage(Team team, float x, float y, float radius, float damage, boolean complete, boolean air, boolean ground, boolean scaled, @Nullable Bullet source){
        Cons<Unit> cons = unit -> {
            if(unit.team == team  || !unit.checkTarget(air, ground) || !unit.hittable() || !unit.within(x, y, radius + (scaled ? unit.hitSize / 2f : 0f))){
                return;
            }

            boolean dead = unit.dead;

            float amount = calculateDamage(scaled ? Math.max(0, unit.dst(x, y) - unit.type.hitSize/2) : unit.dst(x, y), radius, damage);
            unit.damage(amount);

            if(source != null){
                Events.fire(bulletDamageEvent.set(unit, source));
                unit.controller().hit(source);

                if(!dead && unit.dead){
                    Events.fire(new UnitBulletDestroyEvent(unit, source));
                }
            }
            //TODO better velocity displacement
            float dst = vec.set(unit.x - x, unit.y - y).len();
            unit.vel.add(vec.setLength((1f - dst / radius) * 2f / unit.mass()));

            if(complete && damage >= 9999999f && unit.isPlayer()){
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
                tileDamage(team, World.toTile(x), World.toTile(y), radius / tilesize, damage * (source == null ? 1f : source.type.buildingDamageMultiplier), source);
            }else{
                completeDamage(team, x, y, radius, damage);
            }
        }
    }

    public static void tileDamage(Team team, int x, int y, float baseRadius, float damage){
        tileDamage(team, x, y, baseRadius, damage, null);
    }

    public static void tileDamage(Team team, int x, int y, float baseRadius, float damage, @Nullable Bullet source){
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
            float radius = Math.min(baseRadius, 100), rad2 = radius * radius;
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
                    if(source != null){
                        build.damage(source, team, e.value);
                    }else{
                        build.damage(team, e.value);
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
                if(tile != null && tile.build != null && (team == null ||team.isEnemy(tile.team())) && dx*dx + dy*dy <= trad*trad){
                    tile.build.damage(team, damage);
                }
            }
        }
    }

    private static float calculateDamage(float dist, float radius, float damage){
        float falloff = 0.4f;
        float scaled = Mathf.lerp(1f - dist / radius, 1f, falloff);
        return damage * scaled;
    }

    /** @return resulting armor calculated based off of damage */
    public static float applyArmor(float damage, float armor){
        return Math.max(damage - armor, minArmorDamage * damage);
    }

    public static class Collided{
        public float x, y;
        public Teamc target;

        public Collided set(float x, float y, Teamc target){
            this.x = x;
            this.y = y;
            this.target = target;
            return this;
        }
    }
}
