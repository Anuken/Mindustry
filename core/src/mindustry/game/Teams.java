package mindustry.game;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.ai.*;
import mindustry.gen.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

/** Class for various team-based utilities. */
public class Teams{
    /** Maps team IDs to team data. */
    private TeamData[] map = new TeamData[256];
    /** Active teams. */
    private Seq<TeamData> active = new Seq<>();

    public Teams(){
        active.add(get(Team.crux));
    }

    public @Nullable CoreEntity closestEnemyCore(float x, float y, Team team){
        for(Team enemy : team.enemies()){
            CoreEntity tile = Geometry.findClosest(x, y, enemy.cores());
            if(tile != null) return tile;
        }
        return null;
    }

    public @Nullable CoreEntity closestCore(float x, float y, Team team){
        return Geometry.findClosest(x, y, get(team).cores);
    }

    public Team[] enemiesOf(Team team){
        return get(team).enemies;
    }

    public boolean eachEnemyCore(Team team, Boolf<CoreEntity> ret){
        for(TeamData data : active){
            if(areEnemies(team, data.team)){
                for(CoreEntity tile : data.cores){
                    if(ret.get(tile)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void eachEnemyCore(Team team, Cons<Building> ret){
        for(TeamData data : active){
            if(areEnemies(team, data.team)){
                for(Building tile : data.cores){
                    ret.get(tile);
                }
            }
        }
    }

    /** Returns team data by type. */
    public TeamData get(Team team){
        if(map[team.id] == null){
            map[team.id] = new TeamData(team);
        }
        return map[team.id];
    }

    public Seq<CoreEntity> playerCores(){
        return get(state.rules.defaultTeam).cores;
    }

    /** Do not modify! */
    public Seq<CoreEntity> cores(Team team){
        return get(team).cores;
    }

    /** Returns whether a team is active, e.g. whether it has any cores remaining. */
    public boolean isActive(Team team){
        //the enemy wave team is always active
        return get(team).active();
    }

    /** Returns whether {@param other} is an enemy of {@param #team}. */
    public boolean areEnemies(Team team, Team other){
        return team != other;
    }

    public boolean canInteract(Team team, Team other){
        return team == other || other == Team.derelict;
    }

    /** Do not modify. */
    public Seq<TeamData> getActive(){
        active.removeAll(t -> !t.active());
        return active;
    }

    public void registerCore(CoreEntity core){
        TeamData data = get(core.team());
        //add core if not present
        if(!data.cores.contains(core)){
            data.cores.add(core);
        }

        //register in active list if needed
        if(data.active() && !active.contains(data)){
            active.add(data);
            updateEnemies();
            indexer.updateTeamIndex(data.team);
        }
    }

    public void unregisterCore(CoreEntity entity){
        TeamData data = get(entity.team());
        //remove core
        data.cores.remove(entity);
        //unregister in active list
        if(!data.active()){
            active.remove(data);
            updateEnemies();
        }
    }

    private void updateEnemies(){
        if(state.rules.waves && !active.contains(get(state.rules.waveTeam))){
            active.add(get(state.rules.waveTeam));
        }

        for(TeamData data : active){
            Seq<Team> enemies = new Seq<>();

            for(TeamData other : active){
                if(areEnemies(data.team, other.team)){
                    enemies.add(other.team);
                }
            }

            data.enemies = enemies.toArray(Team.class);
        }
    }

    public class TeamData{
        public final Seq<CoreEntity> cores = new Seq<>();
        public final Team team;
        public final BaseAI ai;
        public Team[] enemies = {};
        public Queue<BlockPlan> blocks = new Queue<>();

        public TeamData(Team team){
            this.team = team;
            this.ai = new BaseAI(this);
        }

        public boolean active(){
            return (team == state.rules.waveTeam && state.rules.waves) || cores.size > 0;
        }

        public boolean hasCore(){
            return cores.size > 0;
        }

        public boolean noCores(){
            return cores.isEmpty();
        }

        public @Nullable CoreEntity core(){
            return cores.isEmpty() ? null : cores.first();
        }

        /** @return whether this team is controlled by the AI and builds bases. */
        public boolean hasAI(){
            return state.rules.attackMode && team.rules().ai;
        }

        @Override
        public String toString(){
            return "TeamData{" +
            "cores=" + cores +
            ", team=" + team +
            '}';
        }
    }

    /** Represents a block made by this team that was destroyed somewhere on the map.
     * This does not include deconstructed blocks.*/
    public static class BlockPlan{
        public final short x, y, rotation, block;
        public final Object config;

        public BlockPlan(int x, int y, short rotation, short block, Object config){
            this.x = (short)x;
            this.y = (short)y;
            this.rotation = rotation;
            this.block = block;
            this.config = config;
        }
    }
}
