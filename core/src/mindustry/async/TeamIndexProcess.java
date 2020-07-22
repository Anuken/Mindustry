package mindustry.async;

import arc.math.geom.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;

import java.util.*;

/** Creates quadtrees per unit team. */
public class TeamIndexProcess implements AsyncProcess{
    private QuadTree<Unit>[] trees = new QuadTree[Team.all.length];
    private int[] counts = new int[Team.all.length];
    private int[][] typeCounts = new int[Team.all.length][0];
    private int[][] activeCounts = new int[Team.all.length][0];

    public QuadTree<Unit> tree(Team team){
        if(trees[team.id] == null) trees[team.id] = new QuadTree<>(Vars.world.getQuadBounds(new Rect()));

        return trees[team.id];
    }

    public int count(Team team){
        return counts[team.id];
    }

    public int countType(Team team, UnitType type){
        return typeCounts[team.id].length <= type.id ? 0 : typeCounts[team.id][type.id];
    }

    public int countActive(Team team, UnitType type){
        return activeCounts[team.id].length <= type.id ? 0 : activeCounts[team.id][type.id];
    }

    public void updateCount(Team team, UnitType type, int amount){
        counts[team.id] += amount;
        if(typeCounts[team.id].length <= type.id){
            typeCounts[team.id] = new int[Vars.content.units().size];
        }
        typeCounts[team.id][type.id] += amount;
    }

    public void updateActiveCount(Team team, UnitType type, int amount){
        if(activeCounts[team.id].length <= type.id){
            activeCounts[team.id] = new int[Vars.content.units().size];
        }
        activeCounts[team.id][type.id] += amount;
    }

    @Override
    public void reset(){
        counts = new int[Team.all.length];
        trees = new QuadTree[Team.all.length];
    }

    @Override
    public void begin(){

        for(Team team : Team.all){
            if(trees[team.id] != null){
                trees[team.id].clear();
            }

            Arrays.fill(typeCounts[team.id], 0);
            Arrays.fill(activeCounts[team.id], 0);
        }

        Arrays.fill(counts, 0);

        for(Unit unit : Groups.unit){
            tree(unit.team).insert(unit);

            updateCount(unit.team, unit.type(), 1);
            if(!unit.deactivated) updateActiveCount(unit.team, unit.type(), 1);
        }
    }

    @Override
    public boolean shouldProcess(){
        return false;
    }
}
