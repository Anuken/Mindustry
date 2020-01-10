package mindustry.entities;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Utility class for unit and team interactions.*/
public class Units{
    private static Rect hitrect = new Rect();
    private static Unit result;
    private static float cdist;
    private static boolean boolResult;

    /** @return whether this player can interact with a specific tile. if either of these are null, returns true.*/
    public static boolean canInteract(Player player, Tile tile){
        return player == null || tile == null || tile.interactable(player.getTeam());
    }

    /**
     * Validates a target.
     * @param target The target to validate
     * @param team The team of the thing doing tha targeting
     * @param x The X position of the thing doign the targeting
     * @param y The Y position of the thing doign the targeting
     * @param range The maximum distance from the target X/Y the targeter can be for it to be valid
     * @return whether the target is invalid
     */
    public static boolean invalidateTarget(TargetTrait target, Team team, float x, float y, float range){
        return target == null || (range != Float.MAX_VALUE && !target.withinDst(x, y, range)) || target.getTeam() == team || !target.isValid();
    }

    /** See {@link #invalidateTarget(TargetTrait, Team, float, float, float)} */
    public static boolean invalidateTarget(TargetTrait target, Team team, float x, float y){
        return invalidateTarget(target, team, x, y, Float.MAX_VALUE);
    }

    /** See {@link #invalidateTarget(TargetTrait, Team, float, float, float)} */
    public static boolean invalidateTarget(TargetTrait target, Unit targeter){
        return invalidateTarget(target, targeter.getTeam(), targeter.x, targeter.y, targeter.getWeapon().bullet.range());
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
            if(!unit.isFlying()){
                unit.hitbox(hitrect);

                if(hitrect.overlaps(x, y, width, height)){
                    boolResult = true;
                }
            }
        });

        return boolResult;
    }

    /** Returns the neareset damaged tile. */
    public static TileEntity findDamagedTile(Team team, float x, float y){
        Tile tile = Geometry.findClosest(x, y, indexer.getDamaged(team));
        return tile == null ? null : tile.entity;
    }

    /** Returns the neareset ally tile in a range. */
    public static TileEntity findAllyTile(Team team, float x, float y, float range, Boolf<Tile> pred){
        return indexer.findTile(team, x, y, range, pred);
    }

    /** Returns the neareset enemy tile in a range. */
    public static TileEntity findEnemyTile(Team team, float x, float y, float range, Boolf<Tile> pred){
        if(team == Team.derelict) return null;

        return indexer.findEnemyTile(team, x, y, range, pred);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static TargetTrait closestTarget(Team team, float x, float y, float range){
        return closestTarget(team, x, y, range, Unit::isValid);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static TargetTrait closestTarget(Team team, float x, float y, float range, Boolf<Unit> unitPred){
        return closestTarget(team, x, y, range, unitPred, t -> true);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static TargetTrait closestTarget(Team team, float x, float y, float range, Boolf<Unit> unitPred, Boolf<Tile> tilePred){
        if(team == Team.derelict) return null;

        Unit unit = closestEnemy(team, x, y, range, unitPred);
        if(unit != null){
            return unit;
        }else{
            return findEnemyTile(team, x, y, range, tilePred);
        }
    }

    /** Returns the closest enemy of this team. Filter by predicate. */
    public static Unit closestEnemy(Team team, float x, float y, float range, Boolf<Unit> predicate){
        if(team == Team.derelict) return null;

        result = null;
        cdist = 0f;

        nearbyEnemies(team, x - range, y - range, range*2f, range*2f, e -> {
            if(e.isDead() || !predicate.get(e)) return;

            float dst2 = Mathf.dst2(e.x, e.y, x, y);
            if(dst2 < range*range && (result == null || dst2 < cdist)){
                result = e;
                cdist = dst2;
            }
        });

        return result;
    }

    /** Returns the closest ally of this team. Filter by predicate. */
    public static Unit closest(Team team, float x, float y, float range, Boolf<Unit> predicate){
        result = null;
        cdist = 0f;

        nearby(team, x, y, range, e -> {
            if(!predicate.get(e)) return;

            float dist = Mathf.dst2(e.x, e.y, x, y);
            if(result == null || dist < cdist){
                result = e;
                cdist = dist;
            }
        });

        return result;
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(Team team, float x, float y, float width, float height, Cons<Unit> cons){
        unitGroup.intersect(x, y, width, height, u -> {
            if(u.getTeam() == team || team == null){
                cons.get(u);
            }
        });
        playerGroup.intersect(x, y, width, height, player -> {
            if(player.getTeam() == team || team == null){
                cons.get(player);
            }
        });
    }

    /** Iterates over all units in a circle around this position. */
    public static void nearby(Team team, float x, float y, float radius, Cons<Unit> cons){
        unitGroup.intersect(x - radius, y - radius, radius*2f, radius*2f, unit -> {
            if((unit.getTeam() == team || team == null) && unit.withinDst(x, y, radius)){
                cons.get(unit);
            }
        });

        playerGroup.intersect(x - radius, y - radius, radius*2f, radius*2f, unit -> {
            if((unit.getTeam() == team || team == null) && unit.withinDst(x, y, radius)){
                cons.get(unit);
            }
        });
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(float x, float y, float width, float height, Cons<Unit> cons){
        unitGroup.intersect(x, y, width, height, cons);
        playerGroup.intersect(x, y, width, height, cons);
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(Rect rect, Cons<Unit> cons){
        nearby(rect.x, rect.y, rect.width, rect.height, cons);
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, float x, float y, float width, float height, Cons<Unit> cons){
        unitGroup.intersect(x, y, width, height, u -> {
            if(team.isEnemy(u.getTeam())){
                cons.get(u);
            }
        });

        playerGroup.intersect(x, y, width, height, player -> {
            if(team.isEnemy(player.getTeam())){
                cons.get(player);
            }
        });
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, Rect rect, Cons<Unit> cons){
        nearbyEnemies(team, rect.x, rect.y, rect.width, rect.height, cons);
    }

    /** Iterates over all units. */
    public static void all(Cons<Unit> cons){
        unitGroup.all().each(cons);
        playerGroup.all().each(cons);
    }

    public static void each(Team team, Cons<BaseUnit> cons){
        unitGroup.all().each(t -> t.getTeam() == team, cons);
    }

}
