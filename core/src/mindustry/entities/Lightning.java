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
    private static final Seq<Building> buildings = new Seq<>();
    private static final Vec2 nextPosition = new Vec2();
    private static final int maxChain = 8;
    private static final float hitRange = 30f;
    private static boolean makeBullet = true;
    private static boolean buildingHit = false;
    private static boolean insulatedHit = false;
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

    private static void createLightningInternal(@Nullable Bullet hitter, BulletType hitCreate, int seed, Team team, Color color, float damage, float x, float y, float rotation, int length){
        random.setSeed(seed);
        hit.clear();

        Seq<Vec2> lines = new Seq<>();
        makeBullet = true;
        int ignoredUnits = 0;
        insulatedHit = false;

        buildings.clear();
        for(int i = 0; i < length / 2; i++){
            //generate bullet and insert lightning position for drawing
            if(makeBullet){
                hitCreate.create(null, team, x, y, rotation, damage * (hitter == null ? 1f : hitter.damageMultiplier()), 1f, 1f, hitter);
            }
            lines.add(new Vec2(x + Mathf.range(3f), y + Mathf.range(3f)));

            //stop lightning generation if hit conditions are met, pierceCap considers number of objects hit (not total hits)
            if(insulatedHit) break;
            if(hitter != null && hitter.type.pierceCap > 0 && hit.size - ignoredUnits + (hitter.type.pierceBuilding ? buildings.size : 0) >= hitter.type.pierceCap){
                break;
            }
            makeBullet = true;

            //find entities to hit
            rect.setSize(hitRange).setCenter(x, y);
            entities.clear();
            if(hit.size < maxChain || (hitter != null && hit.size <= hitter.type.pierceCap)){
                Units.nearbyEnemies(team, rect, u -> {
                    if(!hit.contains(u.id()) && (hitter == null || u.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround))){
                        entities.add(u);
                    }
                });
            }
            Unit furthest = Geometry.findFurthest(x, y, entities);

            //generate the next position
            boolean unitHit = false;
            if(furthest != null){
                //if the collision exists, set nextPosition to entity
                unitHit = true;
                hit.add(furthest.id());
                nextPosition.x = furthest.x();
                nextPosition.y = furthest.y();
            }else{
                //otherwise, move lightning forward as normal
                rotation += random.range(20f);
                nextPosition.x = x + Angles.trnsx(rotation, hitRange / 2f);
                nextPosition.y = y + Angles.trnsy(rotation, hitRange / 2f);
            }

            //if the lightning has to collide with the first building it meets
            if(hitter != null && !hitter.type.canLightningJump){

                //check for buildings from current (x,y) to next position (nextPosition)
                buildingHit = false;
                insulatedHit = false;
                World.raycastEachWorld(x, y, nextPosition.getX(), nextPosition.getY(), (wx, wy) -> {
                    Tile tile = world.tile(wx, wy);
                    if(tile != null && tile.build != null && tile.team() != team){
                        if(!buildings.contains(tile.build)){
                            if(tile.build.isInsulated()){
                                insulatedHit = true;
                            }
                            buildings.add(tile.build);
                            //if the collision exists, override nextPosition to building
                            nextPosition.x = wx * tilesize;
                            nextPosition.y = wy * tilesize;
                                                        makeBullet = true;
                            buildingHit = true;
                            return true;
                        }else if(!hitter.type.canLightningMHitBuild){
                            makeBullet = false;
                        }
                    }
                    return false;
                });

                //handle case if a unit was initially meant to be hit, but detects a building along the way
                if(unitHit){
                    if(!makeBullet){
                        Tile tile = world.tile(World.toTile(nextPosition.getX()), World.toTile(nextPosition.getY()));
                        if(tile == null || tile.build == null || tile.team() == team){
                            //make the bullet anyway on the unit if there's no buildings
                            makeBullet = true;
                        }else{
                            //otherwise ignore and move on onto other targets
                            ignoredUnits++;
                        }
                    }else if(buildingHit){
                        //the unit is still a candidate for further chaining
                        hit.remove(furthest.id());
                    }
                }

            }else{

                insulatedHit = false;
                World.raycastEachWorld(x, y, nextPosition.getX(), nextPosition.getY(), (wx, wy) -> {
                    Tile tile = world.tile(wx, wy);
                    if(tile != null && tile.build != null && tile.team() != team && tile.build.isInsulated()){
                        insulatedHit = true;
                        //if the collision exists, override nextPosition to building
                        nextPosition.x = wx * tilesize;
                        nextPosition.y = wy * tilesize;
                        return true;
                    }
                    return false;
                });

                //check if a building exists at the targeted location hit
                if(!unitHit && !insulatedHit){
                    Tile tile = world.tile(World.toTile(nextPosition.getX()), World.toTile(nextPosition.getY()));
                    if(tile != null && tile.build != null && tile.team() != team){
                        if(!buildings.contains(tile.build)){
                            buildings.add(tile.build);
                        }else if(hitter != null && !hitter.type.canLightningMHitBuild){
                            makeBullet = false;
                        }
                    }
                }

            }

            //make the next position to current
            x = nextPosition.getX();
            y = nextPosition.getY();
        }

        Fx.lightning.at(x, y, rotation, color, lines);
    }
}
