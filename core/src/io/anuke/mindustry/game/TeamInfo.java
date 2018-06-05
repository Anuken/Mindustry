package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.ThreadArray;
import io.anuke.ucore.util.ThreadSet;

/**Class for various team-based utilities.*/
public class TeamInfo {
    private ObjectMap<Team, TeamData> map = new ObjectMap<>();
    private ThreadSet<Team> allies = new ThreadSet<>(),
            enemies = new ThreadSet<>();
    private ThreadSet<TeamData> allyData = new ThreadSet<>(),
            enemyData = new ThreadSet<>();
    private ThreadSet<TeamData> allTeamData = new ThreadSet<>();
    private ThreadSet<Team> allTeams = new ThreadSet<>();

    /**Returns all teams on a side.*/
    public ObjectSet<TeamData> getTeams(boolean ally) {
        return ally ? allyData : enemyData;
    }

    /**Returns all team data.*/
    public ObjectSet<TeamData> getTeams() {
        return allTeamData;
    }

    /**Register a team.
     * @param team The team type enum.
     * @param ally Whether this team is an ally with the player or an enemy with the player.
     *             In PvP situations with dedicated servers, the sides can be arbitrary.*/
    public void add(Team team, boolean ally){
        if(has(team)) throw new RuntimeException("Can't define team information twice!");

        TeamData data = new TeamData(team, ally);

        if(ally) {
            allies.add(team);
            allyData.add(data);
        }else {
            enemies.add(team);
            enemyData.add(data);
        }

        allTeamData.add(data);
        allTeams.add(team);

        map.put(team, data);
    }

    /**Returns team data by type. Call {@link #has(Team)} first to make sure it's active!*/
    public TeamData get(Team team){
        if(!has(team)) throw new RuntimeException("This team is not active! Check has() before calling get().");
        return map.get(team);
    }

    /**Returns whether the specified team is active, e.g. whether it is participating in the game.*/
    public boolean has(Team team){
        return map.containsKey(team);
    }

    /**Returns a set of all teams that are enemies of this team.
     * For teams not active, an empty set is returned.*/
    public ObjectSet<Team> enemiesOf(Team team) {
        boolean ally = allies.contains(team);
        boolean enemy = enemies.contains(team);

        //this team isn't even in the game, so target everything!
        if(!ally && !enemy) return allTeams;

        return ally ? enemies : allies;
    }

    /**Returns a set of all teams that are enemies of this team.
     * For teams not active, an empty set is returned.
     */
    public ObjectSet<TeamData> enemyDataOf(Team team) {
        boolean ally = allies.contains(team);
        boolean enemy = enemies.contains(team);

        //this team isn't even in the game, so target everything!
        if(!ally && !enemy) return allTeamData;

        return ally ? enemyData : allyData;
    }

    /**Returns whether or not these two teams are enemies.*/
    public boolean areEnemies(Team team, Team other){
        if(team == other) return false; //fast fail to be more efficient
        boolean ally = allies.contains(team);
        boolean ally2 = enemies.contains(other);
        return (ally == ally2) || !allTeams.contains(team); //if it's not in the game, target everything.
    }

    public class TeamData {
        public final ThreadArray<Tile> cores = new ThreadArray<>();
        public final Team team;
        public final boolean ally;

        public TeamData(Team team, boolean ally) {
            this.team = team;
            this.ally = ally;
        }
    }
}
