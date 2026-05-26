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
    private static final Seq<Unit> entities = new Seq<>();
    private static final IntSet hit = new IntSet();
    private static final int maxChain = 8;
    private static final float hitRange = 30f;
    private static boolean bhit = false;
    private static int lastSeed = 0;

    /** Create a lighting branch at a location. Use Team.derelict to damage everyone. */
    public static void create(BulletType bulletCreated, Team team, Color color, float damage, float x, float y, float targetAngle, int length){
        float statusChance = bulletCreated.lightningStatusChance > -1 ? bulletCreated.lightningStatusChance : bulletCreated.statusChance;
        createLightningInternal(null, bulletCreated, lastSeed++, team, color, damage, x, y, targetAngle, length, statusChance);
    }

    /** Create a lighting branch at a location. Use Team.derelict to damage everyone. */
    public static void create(Team team, Color color, float damage, float x, float y, float targetAngle, int length){
        float statusChance = Bullets.damageLightning.lightningStatusChance > -1 ? Bullets.damageLightning.lightningStatusChance : Bullets.damageLightning.statusChance;
        createLightningInternal(null, Bullets.damageLightning, lastSeed++, team, color, damage, x, y, targetAngle, length, statusChance);
    }

    /** Create a lighting branch at a location. Uses bullet parameters. */
    public static void create(Bullet bullet, Color color, float damage, float x, float y, float targetAngle, int length){
        // Is this a necessary line? The bullet parameter is not nullable and never will be.
        BulletType lightningType = bullet == null || bullet.type.lightningType == null ? Bullets.damageLightning : bullet.type.lightningType;
        assert bullet != null; // intellij recommended
        float statusChance = bullet.type.lightningStatusChance > -1 ? bullet.type.lightningStatusChance : bullet.type.statusChance;
        createLightningInternal(bullet, lightningType, lastSeed++, bullet.team, color, damage, x, y, targetAngle, length, statusChance);
    }

    private static void createLightningInternal(@Nullable Bullet hitter, BulletType hitCreate, int seed, Team team, Color color, float damage, float x, float y, float rotation, int length, float statusChance){
        random.setSeed(seed);
        hit.clear();

        Seq<Vec2> lines = new Seq<>();
        bhit = false;

        // Store original statusChance and set it for lightning bullets
        float originalStatusChance = hitCreate.statusChance;
        hitCreate.statusChance = statusChance;

        for(int i = 0; i < length / 2; i++){
            hitCreate.create(null, team, x, y, rotation, damage * (hitter == null ? 1f : hitter.damageMultiplier()), 1f, 1f, hitter);
            lines.add(new Vec2(x + Mathf.range(3f), y + Mathf.range(3f)));

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
                if(bhit) break;
            }

            rect.setSize(hitRange).setCenter(x, y);
            entities.clear();
            if(hit.size < maxChain){
                Units.nearbyEnemies(team, rect, u -> {
                    if(!hit.contains(u.id()) && (hitter == null || u.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround))){
                        entities.add(u);
                    }
                });
            }

            Unit furthest = Geometry.findFurthest(x, y, entities);

            if(furthest != null){
                hit.add(furthest.id());
                x = furthest.x();
                y = furthest.y();
            }else{
                rotation += random.range(20f);
                x += Angles.trnsx(rotation, hitRange / 2f);
                y += Angles.trnsy(rotation, hitRange / 2f);
            }
        }

        Fx.lightning.at(x, y, rotation, color, lines);

        // Restore original statusChance
        hitCreate.statusChance = originalStatusChance;
    }
}
