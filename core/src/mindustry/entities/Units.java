package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Utility class for unit and team interactions.*/
public class Units{
    private static final Rect hitrect = new Rect();
    private static Unit result;
    private static float cdist;
    private static boolean boolResult;

    @Remote(called = Loc.server)
    public static void unitCapDeath(Unit unit){
        if(unit != null){
            unit.dead = true;
            Fx.unitCapKill.at(unit);
            Core.app.post(() -> Call.unitDestroy(unit.id));
        }
    }

    @Remote(called = Loc.server)
    public static void unitDeath(int uid){
        Unit unit = Groups.unit.getByID(uid);

        //if there's no unit don't add it later and get it stuck as a ghost
        if(netClient != null){
            netClient.addRemovedEntity(uid);
        }

        if(unit != null){
            unit.killed();
        }
    }

    //destroys immediately
    @Remote(called = Loc.server)
    public static void unitDestroy(int uid){
        Unit unit = Groups.unit.getByID(uid);

        //if there's no unit don't add it later and get it stuck as a ghost
        if(netClient != null){
            netClient.addRemovedEntity(uid);
        }

        if(unit != null){
            unit.destroy();
        }
    }

    @Remote(called = Loc.server)
    public static void unitDespawn(Unit unit){
        Fx.unitDespawn.at(unit.x, unit.y, 0, unit);
        unit.remove();
    }

    /** @return whether a new instance of a unit of this team can be created. */
    public static boolean canCreate(Team team, UnitType type){
        return team.data().countType(type) < getCap(team);
    }

    public static int getCap(Team team){
        //wave team has no cap
        if((team == state.rules.waveTeam && !state.rules.pvp) || (state.isCampaign() && team == state.rules.waveTeam)){
            return Integer.MAX_VALUE;
        }
        return state.rules.unitCap + indexer.getExtraUnits(team);
    }

    /** @return whether this player can interact with a specific tile. if either of these are null, returns true.*/
    public static boolean canInteract(Player player, Building tile){
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
        return target == null || (range != Float.MAX_VALUE && !target.within(x, y, range)) || (target instanceof Teamc && ((Teamc)target).team() == team) || (target instanceof Healthc && !((Healthc)target).isValid());
    }

    /** See {@link #invalidateTarget(Posc, Team, float, float, float)} */
    public static boolean invalidateTarget(Posc target, Team team, float x, float y){
        return invalidateTarget(target, team, x, y, Float.MAX_VALUE);
    }

    /** See {@link #invalidateTarget(Posc, Team, float, float, float)} */
    public static boolean invalidateTarget(Teamc target, Unit targeter, float range){
        return invalidateTarget(target, targeter.team(), targeter.x(), targeter.y(), range);
    }

    /** Returns whether there are any entities on this tile. */
    public static boolean anyEntities(Tile tile, boolean ground){
        float size = tile.block().size * tilesize;
        return anyEntities(tile.drawx() - size/2f, tile.drawy() - size/2f, size, size, ground);
    }

    /** Returns whether there are any entities on this tile. */
    public static boolean anyEntities(Tile tile){
        return anyEntities(tile, true);
    }

    public static boolean anyEntities(float x, float y, float width, float height){
        return anyEntities(x, y, width, height, true);
    }

    public static boolean anyEntities(float x, float y, float width, float height, boolean ground){
        boolResult = false;

        nearby(x, y, width, height, unit -> {
            if(boolResult) return;
            if((unit.isGrounded() && !unit.type.hovering) == ground){
                unit.hitbox(hitrect);

                if(hitrect.overlaps(x, y, width, height)){
                    boolResult = true;
                }
            }
        });

        return boolResult;
    }

    /** Returns the neareset damaged tile. */
    public static Building findDamagedTile(Team team, float x, float y){
        return Geometry.findClosest(x, y, indexer.getDamaged(team));
    }

    /** Returns the neareset ally tile in a range. */
    public static Building findAllyTile(Team team, float x, float y, float range, Boolf<Building> pred){
        return indexer.findTile(team, x, y, range, pred);
    }

    /** Returns the neareset enemy tile in a range. */
    public static Building findEnemyTile(Team team, float x, float y, float range, Boolf<Building> pred){
        if(team == Team.derelict) return null;

        return indexer.findEnemyTile(team, x, y, range, pred);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range){
        return closestTarget(team, x, y, range, Unit::isValid);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, Boolf<Unit> unitPred){
        return closestTarget(team, x, y, range, unitPred, t -> true);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, Boolf<Unit> unitPred, Boolf<Building> tilePred){
        if(team == Team.derelict) return null;

        Unit unit = closestEnemy(team, x, y, range, unitPred);
        if(unit != null){
            return unit;
        }else{
            return findEnemyTile(team, x, y, range, tilePred);
        }
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc bestTarget(Team team, float x, float y, float range, Boolf<Unit> unitPred, Boolf<Building> tilePred, Sortf sort){
        if(team == Team.derelict) return null;

        Unit unit = bestEnemy(team, x, y, range, unitPred, sort);
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
            if(e.dead() || !predicate.get(e) || e.team == Team.derelict) return;

            float dst2 = e.dst2(x, y);
            if(dst2 < range*range && (result == null || dst2 < cdist)){
                result = e;
                cdist = dst2;
            }
        });

        return result;
    }

    /** Returns the closest enemy of this team using a custom comparison function. Filter by predicate. */
    public static Unit bestEnemy(Team team, float x, float y, float range, Boolf<Unit> predicate, Sortf sort){
        if(team == Team.derelict) return null;

        result = null;
        cdist = 0f;

        nearbyEnemies(team, x - range, y - range, range*2f, range*2f, e -> {
            if(e.dead() || !predicate.get(e) || !e.within(x, y, range)) return;

            float cost = sort.cost(e, x, y);
            if(result == null || cost < cdist){
                result = e;
                cdist = cost;
            }
        });

        return result;
    }

    /** Returns the closest ally of this team. Filter by predicate. No range. */
    public static Unit closest(Team team, float x, float y, Boolf<Unit> predicate){
        result = null;
        cdist = 0f;

        for(Unit e : Groups.unit){
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
    public static Unit closest(Team team, float x, float y, float range, Boolf<Unit> predicate){
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

    /** Returns the closest ally of this team. Filter by predicate.
     * Unlike the closest() function, this only guarantees that unit hitboxes overlap the range. */
    public static Unit closestOverlap(Team team, float x, float y, float range, Boolf<Unit> predicate){
        result = null;
        cdist = 0f;

        nearby(team, x - range, y - range, range*2f, range*2f, e -> {
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
    public static void nearby(Team team, float x, float y, float width, float height, Cons<Unit> cons){
        team.data().tree().intersect(x, y, width, height, cons);
    }

    /** Iterates over all units in a circle around this position. */
    public static void nearby(Team team, float x, float y, float radius, Cons<Unit> cons){
        nearby(team, x - radius, y - radius, radius*2f, radius*2f, unit -> {
            if(unit.within(x, y, radius)){
                cons.get(unit);
            }
        });
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(float x, float y, float width, float height, Cons<Unit> cons){
        Groups.unit.intersect(x, y, width, height, cons);
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(Rect rect, Cons<Unit> cons){
        nearby(rect.x, rect.y, rect.width, rect.height, cons);
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, float x, float y, float width, float height, Cons<Unit> cons){
        Seq<TeamData> data = state.teams.present;
        for(int i = 0; i < data.size; i++){
            if(data.items[i].team != team){
                nearby(data.items[i].team, x, y, width, height, cons);
            }
        }
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, Rect rect, Cons<Unit> cons){
        nearbyEnemies(team, rect.x, rect.y, rect.width, rect.height, cons);
    }

    public interface Sortf{
        float cost(Unit unit, float x, float y);
    }
}
