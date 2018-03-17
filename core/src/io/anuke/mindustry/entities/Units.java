package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Predicate;

import static io.anuke.mindustry.Vars.*;

/**Utility class for unit and team interactions.*/
public class Units {
    private static Rectangle rect = new Rectangle();

    /**Iterates over all units on all teams, including players.*/
    public static void allUnits(Consumer<Unit> cons){
        //check all unit groups first
        for(EntityGroup<BaseUnit> group : unitGroups){
            if(!group.isEmpty()){
                for(BaseUnit unit : group.all()){
                    cons.accept(unit);
                }
            }
        }

        //then check all player groups
        for(Player player : playerGroup.all()){
            cons.accept(player);
        }
    }

    /**Returns the closest enemy of this team. Filter by predicate.*/
    public static Unit getClosestEnemy(Team team, float x, float y, float range, Predicate<Unit> predicate){
        Unit[] result = {null};
        float[] cdist = {0};

        getNearbyEnemies(team, rect, e -> {
            if (!predicate.test(e))
                return;

            float dist = Vector2.dst(e.x, e.y, x, y);
            if (dist < range) {
                if (result[0] == null || dist < cdist[0]) {
                    result[0] = e;
                    cdist[0] = dist;
                }
            }
        });

        return result[0];
    }

    /**Iterates over all units in a rectangle.*/
    public static void getNearby(Rectangle rect, Consumer<Unit> cons){

        for(Team team : Team.values()){
            EntityGroup<BaseUnit> group = unitGroups[team.ordinal()];
            if(!group.isEmpty()){
                Entities.getNearby(group, rect, entity -> cons.accept((Unit)entity));
            }
        }

        //now check all enemy players
        Entities.getNearby(playerGroup, rect, player -> cons.accept((Unit)player));
    }

    /**Iterates over all units that are enemies of this team.*/
    public static void getNearbyEnemies(Team team, Rectangle rect, Consumer<Unit> cons){
        //check if it's an ally team to the 'main team'
        boolean ally = state.allyTeams.contains(team);
        boolean enemy = state.enemyTeams.contains(team);

        //this team isn't even in the game, so target nothing!
        if(!ally && !enemy) return;

        ObjectSet<Team> targets = ally ? state.enemyTeams : state.allyTeams;

        for(Team other : targets){
            EntityGroup<BaseUnit> group = unitGroups[other.ordinal()];
            if(!group.isEmpty()){
                Entities.getNearby(group, rect, entity -> cons.accept((Unit)entity));
            }
        }

        //now check all enemy players
        Entities.getNearby(playerGroup, rect, player -> {
            if(targets.contains(((Player)player).team)){
                cons.accept((Unit)player);
            }
        });
    }

    /**Returns whether these two teams are enemies.*/
    public static boolean areEnemies(Team team, Team other){
        if(team == other) return false; //fast fail to be more efficient
        boolean ally = state.allyTeams.contains(team);
        boolean ally2 = state.allyTeams.contains(other);
        return ally == ally2;
    }
}
