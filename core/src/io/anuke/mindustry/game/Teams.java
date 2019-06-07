package io.anuke.mindustry.game;

import io.anuke.arc.collection.*;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;

/** Class for various team-based utilities. */
public class Teams{
    private TeamData[] map = new TeamData[Team.all.length];

    /**
     * Register a team.
     * @param team The team type enum.
     * @param enemies The array of enemies of this team. Any team not in this array is considered neutral.
     */
    public void add(Team team, Team... enemies){
        map[team.ordinal()] = new TeamData(team, EnumSet.of(enemies));
    }

    /** Returns team data by type. */
    public TeamData get(Team team){
        if(map[team.ordinal()] == null){
            add(team, Array.with(Team.all).select(t -> t != team).toArray(Team.class));
        }
        return map[team.ordinal()];
    }

    /** Returns whether a team is active, e.g. whether it has any cores remaining. */
    public boolean isActive(Team team){
        //the enemy wave team is always active
        return team == Vars.waveTeam || get(team).cores.size > 0;
    }

    /** Returns a set of all teams that are enemies of this team. */
    public EnumSet<Team> enemiesOf(Team team){
        return get(team).enemies;
    }

    /** Returns whether {@param other} is an enemy of {@param #team}. */
    public boolean areEnemies(Team team, Team other){
        return enemiesOf(team).contains(other);
    }

    public class TeamData{
        public final ObjectSet<Tile> cores = new ObjectSet<>();
        public final EnumSet<Team> enemies;
        public final Team team;

        public TeamData(Team team, EnumSet<Team> enemies){
            this.team = team;
            this.enemies = enemies;
        }
    }
}
