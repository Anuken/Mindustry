package mindustry.game;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.Queue;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.*;

import static mindustry.Vars.*;

/** Class for various team-based utilities. */
public class Teams{
    /** Maps team IDs to team data. */
    private TeamData[] map = new TeamData[256];
    /** Active teams. */
    public Seq<TeamData> active = new Seq<>();
    /** Teams with block or unit presence. */
    public Seq<TeamData> present = new Seq<>(TeamData.class);
    /** Current boss units. */
    public Seq<Unit> bosses = new Seq<>();

    public Teams(){
        active.add(get(Team.crux));
    }

    @Nullable
    public CoreBuild closestEnemyCore(float x, float y, Team team){
        for(Team enemy : team.data().coreEnemies){
            CoreBuild tile = Geometry.findClosest(x, y, enemy.cores());
            if(tile != null) return tile;
        }
        return null;
    }

    @Nullable
    public CoreBuild closestCore(float x, float y, Team team){
        return Geometry.findClosest(x, y, get(team).cores);
    }

    public boolean eachEnemyCore(Team team, Boolf<CoreBuild> ret){
        for(TeamData data : active){
            if(team != data.team){
                for(CoreBuild tile : data.cores){
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
            if(team != data.team){
                for(Building tile : data.cores){
                    ret.get(tile);
                }
            }
        }
    }

    /** Returns team data by type. */
    public TeamData get(Team team){
        if(map[team.id] == null) map[team.id] = new TeamData(team);
        return map[team.id];
    }

    public Seq<CoreBuild> playerCores(){
        return get(state.rules.defaultTeam).cores;
    }

    /** Do not modify! */
    public Seq<CoreBuild> cores(Team team){
        return get(team).cores;
    }

    /** Returns whether a team is active, e.g. whether it has any cores remaining. */
    public boolean isActive(Team team){
        //the enemy wave team is always active
        return get(team).active();
    }

    public boolean canInteract(Team team, Team other){
        return team == other || other == Team.derelict;
    }

    /** Do not modify. */
    public Seq<TeamData> getActive(){
        active.removeAll(t -> !t.active());
        return active;
    }

    public void registerCore(CoreBuild core){
        TeamData data = get(core.team);
        //add core if not present
        if(!data.cores.contains(core)){
            data.cores.add(core);
        }

        //register in active list if needed
        if(data.active() && !active.contains(data)){
            active.add(data);
            updateEnemies();
        }
    }

    public void unregisterCore(CoreBuild entity){
        TeamData data = get(entity.team);
        data.cores.remove(entity);
        //unregister in active list
        if(!data.active()){
            active.remove(data);
            updateEnemies();
        }
    }

    private void count(Unit unit){
        unit.team.data().updateCount(unit.type, 1);

        if(unit instanceof Payloadc payloadc){
            payloadc.payloads().each(p -> {
                if(p instanceof UnitPayload payload){
                    count(payload.unit);
                }
            });
        }
    }

    public void updateTeamStats(){
        present.clear();
        bosses.clear();

        for(Team team : Team.all){
            TeamData data = team.data();

            data.presentFlag = false;
            data.unitCount = 0;
            data.units.clear();
            if(data.tree != null){
                data.tree.clear();
            }

            if(data.typeCounts != null){
                Arrays.fill(data.typeCounts, 0);
            }

            //clear old unit records
            if(data.unitsByType != null){
                for(int i = 0; i < data.unitsByType.length; i++){
                    if(data.unitsByType[i] != null){
                        data.unitsByType[i].clear();
                    }
                }
            }
        }

        //update presence flag.
        Groups.build.each(b -> b.team.data().presentFlag = true);

        for(Unit unit : Groups.unit){
            if(unit.type == null) continue;
            TeamData data = unit.team.data();
            data.tree().insert(unit);
            data.units.add(unit);
            data.presentFlag = true;

            if(unit.team == state.rules.waveTeam && unit.isBoss()){
                bosses.add(unit);
            }

            if(data.unitsByType == null || data.unitsByType.length <= unit.type.id){
                data.unitsByType = new Seq[content.units().size];
            }

            if(data.unitsByType[unit.type.id] == null){
                data.unitsByType[unit.type.id] = new Seq<>();
            }

            data.unitsByType[unit.type.id].add(unit);

            count(unit);
        }

        //update presence of each team.
        for(Team team : Team.all){
            TeamData data = team.data();

            if(data.presentFlag || data.active()){
                present.add(data);
            }
        }
    }

    private void updateEnemies(){
        if(state.rules.waves && !active.contains(get(state.rules.waveTeam))){
            active.add(get(state.rules.waveTeam));
        }

        for(TeamData data : active){
            Seq<Team> enemies = new Seq<>();

            for(TeamData other : active){
                if(data.team != other.team){
                    enemies.add(other.team);
                }
            }

            data.coreEnemies = enemies.toArray(Team.class);
        }
    }

    public static class TeamData{
        public final Seq<CoreBuild> cores = new Seq<>();
        public final Team team;
        public final BaseAI ai;

        private boolean presentFlag;

        /** Enemies with cores or spawn points. */
        public Team[] coreEnemies = {};
        /** Planned blocks for drones. This is usually only blocks that have been broken. */
        public Queue<BlockPlan> blocks = new Queue<>();
        /** The current command for units to follow. */
        public UnitCommand command = UnitCommand.attack;
        /** Target items to mine. */
        public Seq<Item> mineItems = Seq.with(Items.copper, Items.lead, Items.titanium, Items.thorium);

        /** Quadtree for all buildings of this team. Null if not active. */
        @Nullable
        public QuadTree<Building> buildings;
        /** Current unit cap. Do not modify externally. */
        public int unitCap;
        /** Total unit count. */
        public int unitCount;
        /** Counts for each type of unit. Do not access directly. */
        @Nullable
        public int[] typeCounts;
        /** Quadtree for units of this team. Do not access directly. */
        @Nullable
        public QuadTree<Unit> tree;
        /** Units of this team. Updated each frame. */
        public Seq<Unit> units = new Seq<>();
        /** Units of this team by type. Updated each frame. */
        @Nullable
        public Seq<Unit>[] unitsByType;

        public TeamData(Team team){
            this.team = team;
            this.ai = new BaseAI(this);
        }

        /** Destroys this team's presence on the map, killing part of its buildings and converting everything to 'derelict'. */
        public void destroyToDerelict(){

            //grab all buildings from quadtree.
            var builds = new Seq<Building>();
            if(buildings != null){
                buildings.getObjects(builds);
            }

            //convert all team tiles to neutral, randomly killing them
            for(var b : builds){
                //TODO this may cause a lot of packet spam, optimize?
                Call.setTeam(b, Team.derelict);

                if(Mathf.chance(0.25)){
                    Time.run(Mathf.random(0f, 60f * 6f), b::kill);
                }
            }

            //kill all units randomly
            units.each(u -> Time.run(Mathf.random(0f, 60f * 5f), () -> {
                //ensure unit hasn't switched teams for whatever reason
                if(u.team == team){
                    u.kill();
                }
            }));
        }

        @Nullable
        public Seq<Unit> unitCache(UnitType type){
            if(unitsByType == null || unitsByType.length <= type.id || unitsByType[type.id] == null) return null;
            return unitsByType[type.id];
        }

        public void updateCount(UnitType type, int amount){
            if(type == null) return;
            unitCount = Math.max(amount + unitCount, 0);
            if(typeCounts == null || typeCounts.length <= type.id){
                typeCounts = new int[Vars.content.units().size];
            }
            typeCounts[type.id] = Math.max(amount + typeCounts[type.id], 0);
        }

        public QuadTree<Unit> tree(){
            if(tree == null) tree = new QuadTree<>(Vars.world.getQuadBounds(new Rect()));
            return tree;
        }

        public int countType(UnitType type){
            return typeCounts == null || typeCounts.length <= type.id ? 0 : typeCounts[type.id];
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

        @Nullable
        public CoreBuild core(){
            return cores.isEmpty() ? null : cores.first();
        }

        /** @return whether this team is controlled by the AI and builds bases. */
        public boolean hasAI(){
            return team.rules().ai;
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
        public boolean removed;

        public BlockPlan(int x, int y, short rotation, short block, Object config){
            this.x = (short)x;
            this.y = (short)y;
            this.rotation = rotation;
            this.block = block;
            this.config = config;
        }

        @Override
        public String toString(){
            return "BlockPlan{" +
            "x=" + x +
            ", y=" + y +
            ", rotation=" + rotation +
            ", block=" + block +
            ", config=" + config +
            '}';
        }
    }
}
