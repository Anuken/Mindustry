package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.EnumSet;
import io.anuke.ucore.util.ThreadSet;

/**
 * Class for various team-based utilities.
 */
public class Teams{
    private TeamData[] map = new TeamData[Team.all.length];

    /**
     * Register a team.
     *
     * @param team The team type enum.
     * @param enemies The array of enemies of this team. Any team not in this array is considered neutral.
     */
    public void add(Team team, Team... enemies){
        map[team.ordinal()] = new TeamData(team, EnumSet.of(enemies));
    }

    /**Returns team data by type.*/
    public TeamData get(Team team){
        if(map[team.ordinal()] == null){
            //By default, a non-defined team will be enemies of everything.
            Team[] others = new Team[Team.all.length-1];
            for(int i = 0, j = 0; i < Team.all.length; i++){
                if(Team.all[i] != team) others[j++] = Team.all[i];
            }
            add(team, others);
        }
        return map[team.ordinal()];
    }

    /**Returns whether a team is active, e.g. whether it has any cores remaining.*/
    public boolean isActive(Team team){
        //the enemy wave team is always active
        return (!Vars.state.mode.disableWaves && team == Vars.waveTeam) || get(team).cores.size > 0;
    }

    /**Returns a set of all teams that are enemies of this team.*/
    public EnumSet<Team> enemiesOf(Team team){
        return get(team).enemies;
    }

    /**Returns whether {@param other} is an enemy of {@param #team}.*/
    public boolean areEnemies(Team team, Team other){
        return enemiesOf(team).contains(other);
    }

    public class TeamData{
        public final ObjectSet<Tile> cores = new ThreadSet<>();
        public final EnumSet<Team> enemies;
        public final Team team;

        public TeamData(Team team, EnumSet<Team> enemies){
            this.team = team;
            this.enemies = enemies;
        }
    }
}
