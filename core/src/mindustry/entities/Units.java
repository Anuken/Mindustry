package mindustry.entities;

import arc.func.*;
import arc.math.geom.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Utility class for unit and team interactions.*/
public class Units{
    private static Rect hitrect = new Rect();
    private static Unitc result;
    private static float cdist;
    private static boolean boolResult;

    /** @return whether this player can interact with a specific tile. if either of these are null, returns true.*/
    public static boolean canInteract(Playerc player, Tilec tile){
        return player == null || tile == null || tile.interactable(player.team());
    }

    /**
     * Validates a target.
     * @param target The target to validate
     * @param team The team of the thing doing tha targeting
     * @param x The X position of the thing doing the targeting
     * @param y The Y position of the thing doing the targeting
     * @param range The maximum distance from the target X/Y the targeter can be for it to be valid
     * @return whether the target is invalid
     */
    public static boolean invalidateTarget(Posc target, Team team, float x, float y, float range){
        return target == null || !target.isAdded() || (range != Float.MAX_VALUE && !target.within(x, y, range)) || (target instanceof Teamc && ((Teamc)target).team() == team) || (target instanceof Healthc && !((Healthc)target).isValid());
    }

    /** See {@link #invalidateTarget(Posc, Team, float, float, float)} */
    public static boolean invalidateTarget(Posc target, Team team, float x, float y){
        return invalidateTarget(target, team, x, y, Float.MAX_VALUE);
    }

    /** See {@link #invalidateTarget(Posc, Team, float, float, float)} */
    public static boolean invalidateTarget(Teamc target, Unitc targeter, float range){
        return invalidateTarget(target, targeter.team(), targeter.x(), targeter.y(), range);
    }

    /** Returns whether there are any entities on this tile. */
    public static boolean anyEntities(Tile tile){
        float size = tile.block().size * tilesize;
        return anyEntities(tile.drawx() - size/2f, tile.drawy() - size/2f, size, size);
    }

    public static boolean anyEntities(float x, float y, float width, float height){
        boolResult = false;

        nearby(x, y, width, height, unit -> {
            if(boolResult) return;
            if(unit.isGrounded()){
                unit.hitbox(hitrect);

                if(hitrect.overlaps(x, y, width, height)){
                    boolResult = true;
                }
            }
        });

        return boolResult;
    }

    /** Returns the neareset damaged tile. */
    public static Tilec findDamagedTile(Team team, float x, float y){
        Tile tile = Geometry.findClosest(x, y, indexer.getDamaged(team));
        return tile == null ? null : tile.entity;
    }

    /** Returns the neareset ally tile in a range. */
    public static Tilec findAllyTile(Team team, float x, float y, float range, Boolf<Tilec> pred){
        return indexer.findTile(team, x, y, range, pred);
    }

    /** Returns the neareset enemy tile in a range. */
    public static Tilec findEnemyTile(Team team, float x, float y, float range, Boolf<Tilec> pred){
        if(team == Team.derelict) return null;

        return indexer.findEnemyTile(team, x, y, range, pred);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range){
        return closestTarget(team, x, y, range, Unitc::isValid);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, Boolf<Unitc> unitPred){
        return closestTarget(team, x, y, range, unitPred, t -> true);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, Boolf<Unitc> unitPred, Boolf<Tilec> tilePred){
        if(team == Team.derelict) return null;

        Unitc unit = closestEnemy(team, x, y, range, unitPred);
        if(unit != null){
            return unit;
        }else{
            return findEnemyTile(team, x, y, range, tilePred);
        }
    }

    /** Returns the closest enemy of this team. Filter by predicate. */
    public static Unitc closestEnemy(Team team, float x, float y, float range, Boolf<Unitc> predicate){
        if(team == Team.derelict) return null;

        result = null;
        cdist = 0f;

        nearbyEnemies(team, x - range, y - range, range*2f, range*2f, e -> {
            if(e.dead() || !predicate.get(e)) return;

            float dst2 = e.dst2(x, y);
            if(dst2 < range*range && (result == null || dst2 < cdist)){
                result = e;
                cdist = dst2;
            }
        });

        return result;
    }

    /** Returns the closest ally of this team. Filter by predicate. No range. */
    public static Unitc closest(Team team, float x, float y, Boolf<Unitc> predicate){
        result = null;
        cdist = 0f;

        //TODO optimize
        for(Unitc e : Groups.unit){
            if(!predicate.get(e) || e.team() != team) continue;

            float dist = e.dst2(x, y);
            if(result == null || dist < cdist){
                result = e;
                cdist = dist;
            }
        }

        return result;
    }

    /** Returns the closest ally of this team. Filter by predicate. */
    public static Unitc closest(Team team, float x, float y, float range, Boolf<Unitc> predicate){
        result = null;
        cdist = 0f;

        nearby(team, x, y, range, e -> {
            if(!predicate.get(e)) return;

            float dist = e.dst2(x, y);
            if(result == null || dist < cdist){
                result = e;
                cdist = dist;
            }
        });

        return result;
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(Team team, float x, float y, float width, float height, Cons<Unitc> cons){
        teamIndex.tree(team).intersect(height, x, y, width, cons);
    }

    /** Iterates over all units in a circle around this position. */
    public static void nearby(Team team, float x, float y, float radius, Cons<Unitc> cons){
        nearby(team, x - radius, y - radius, radius*2f, radius*2f, unit -> {
            if(unit.within(x, y, radius)){
                cons.get(unit);
            }
        });
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(float x, float y, float width, float height, Cons<Unitc> cons){
        Groups.unit.intersect(x, y, width, height, cons);
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(Rect rect, Cons<Unitc> cons){
        nearby(rect.x, rect.y, rect.width, rect.height, cons);
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, float x, float y, float width, float height, Cons<Unitc> cons){
        for(Team enemy : state.teams.enemiesOf(team)){
            nearby(enemy, x, y, width, height, cons);
        }
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, Rect rect, Cons<Unitc> cons){
        nearbyEnemies(team, rect.x, rect.y, rect.width, rect.height, cons);
    }

}
