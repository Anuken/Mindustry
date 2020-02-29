package mindustry.game;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.entities.type.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

/** Class for various team-based utilities. */
public class Teams{
    /** Maps team IDs to team data. */
    private TeamData[] map = new TeamData[256];
    /** Active teams. */
    private Array<TeamData> active = new Array<>();

    public Teams(){
        active.add(get(Team.crux));
    }

    public @Nullable CoreEntity closestEnemyCore(float x, float y, Team team){
        for(TeamData data : active){
            if(areEnemies(team, data.team)){
                CoreEntity tile = Geometry.findClosest(x, y, data.cores);
                if(tile != null){
                    return tile;
                }
            }
        }
        return null;
    }

    public @Nullable CoreEntity closestCore(float x, float y, Team team){
        return Geometry.findClosest(x, y, get(team).cores);
    }

    public Array<Team> enemiesOf(Team team){
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

    public void eachEnemyCore(Team team, Cons<TileEntity> ret){
        for(TeamData data : active){
            if(areEnemies(team, data.team)){
                for(TileEntity tile : data.cores){
                    ret.get(tile);
                }
            }
        }
    }

    /** Returns team data by type. */
    public TeamData get(Team team){
        if(map[Pack.u(team.id)] == null){
            map[Pack.u(team.id)] = new TeamData(team);
        }
        return map[Pack.u(team.id)];
    }

    public Array<CoreEntity> playerCores(){
        return get(state.rules.defaultTeam).cores;
    }

    /** Do not modify! */
    public Array<CoreEntity> cores(Team team){
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
    public Array<TeamData> getActive(){
        active.removeAll(t -> !t.active());
        return active;
    }

    public void registerCore(CoreEntity core){
        TeamData data = get(core.getTeam());
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
        TeamData data = get(entity.getTeam());
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
            data.enemies.clear();
            for(TeamData other : active){
                if(areEnemies(data.team, other.team)){
                    data.enemies.add(other.team);
                }
            }
        }
    }

    public class TeamData{
        public final Array<CoreEntity> cores = new Array<>();
        public final Array<Team> enemies = new Array<>();
        public final Team team;
        public Queue<BrokenBlock> brokenBlocks = new Queue<>();

        public TeamData(Team team){
            this.team = team;
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

        public CoreEntity core(){
            return cores.first();
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
    public static class BrokenBlock{
        public final short x, y, rotation, block;
        public final int config;

        public BrokenBlock(short x, short y, short rotation, short block, int config){
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.block = block;
            this.config = config;
        }
    }
}
