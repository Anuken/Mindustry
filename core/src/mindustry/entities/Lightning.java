package mindustry.entities;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Lightning{
    private static final Rand random = new Rand();
    private static final Rect rect = new Rect();
    /** Stored unit ids that have already been hit. */
    private static final IntSet hit = new IntSet();
    private static final int maxChain = 8;
    /** New entities to search for while scanning. */
    private static final Seq<Unit> entities = new Seq<>();
    /** Rect side length to scan when searching for entities to hit. */
    private static final float hitRange = 30f;
    /** Number of times the same entity has been hit */
    private static int hitCount = 0;
    private static boolean bhit = false;
    private static int lastSeed = 0;

    /** Create a lighting branch at a location. Use Team.derelict to damage everyone. */
    public static void create(BulletType bulletCreated, Team team, Color color, float damage, float x, float y, float targetAngle, int length){
        createLightningInternal(null, bulletCreated, lastSeed++, team, color, damage, x, y, targetAngle, length);
    }

    /** Create a lighting branch at a location. Use Team.derelict to damage everyone. */
    public static void create(Team team, Color color, float damage, float x, float y, float targetAngle, int length){
        createLightningInternal(null, Bullets.damageLightning, lastSeed++, team, color, damage, x, y, targetAngle, length);
    }

    /** Create a lighting branch at a location. Uses bullet parameters. */
    public static void create(Bullet bullet, Color color, float damage, float x, float y, float targetAngle, int length){
        createLightningInternal(bullet, bullet == null || bullet.type.lightningType == null ? Bullets.damageLightning : bullet.type.lightningType, lastSeed++, bullet.team, color, damage, x, y, targetAngle, length);
    }

    private static void createLightningInternal(@Nullable Bullet hitter, BulletType nodeType, int seed, Team team, Color color, float damage, float x, float y, float rotation, int length){
        random.setSeed(seed);
        hit.clear();

        Seq<Vec2> lines = new Seq<>();
        hitCount = 0;
        bhit = false;

        for(int i = 0; i < length / 2; i++){
            if(hitter == null || (hitter.type.lightningHits > 0 && hitCount < hitter.type.lightningHits)){
                nodeType.create(null, team, x, y, rotation, damage * (hitter == null ? 1f : hitter.damageMultiplier()), 1f, 1f, null);
                hitCount++;
            }
            lines.add(new Vec2(x + Mathf.range(3f), y + Mathf.range(3f))); //visuals are added regardless of bullet node (what actually deals damage)

            //scan buildings
            if(lines.size > 1){
                bhit = false;
                Vec2 from = lines.get(lines.size - 2);
                Vec2 to = lines.get(lines.size - 1);
                World.raycastEach(World.toTile(from.getX()), World.toTile(from.getY()), World.toTile(to.getX()), World.toTile(to.getY()), (wx, wy) -> {

                    Tile tile = world.tile(wx, wy);
                    if(tile != null && (tile.build != null && tile.build.isInsulated()) && tile.team() != team){
                        bhit = true;
                        //snap it instead of removing
                        lines.get(lines.size - 1).set(wx * tilesize, wy * tilesize);
                        return true;
                    }
                    return false;
                });
                if(bhit) break; //insulated found
            }

            //scan units
            rect.setSize(hitRange).setCenter(x, y);
            entities.clear();
            if(hit.size < maxChain){
                Units.nearbyEnemies(team, rect, u -> {
                    if(!hit.contains(u.id()) && (hitter == null || u.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround))){
                        entities.add(u);
                    }
                });
            }

            if(hitter != null && hitter.type.pierceCap > 0 && hit.size >= hitter.type.pierceCap) break;

            //calculate next jump
            Unit furthest = Geometry.findFurthest(x, y, entities);
            if(furthest != null && furthest.within(x, y, length * 1.5f)){
                hit.add(furthest.id());
                x = furthest.x();
                y = furthest.y();
                hitCount = 0;
            }else{
                rotation += random.range(20f);
                x += Angles.trnsx(rotation, hitRange / 2f);
                y += Angles.trnsy(rotation, hitRange / 2f);
            }
        }

        Fx.lightning.at(x, y, rotation, color, lines);
    }
}