package mindustry.async;

import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;

/** Creates quadtrees per unit team. */
public class TeamIndexProcess implements AsyncProcess{
    private QuadTree<Unitc>[] trees = new QuadTree[Team.all().length];
    private Array<Team> active = new Array<>();
    private int[] counts = new int[Team.all().length];

    public QuadTree<Unitc> tree(Team team){
        if(trees[team.uid] == null) trees[team.uid] = new QuadTree<>(Vars.world.getQuadBounds(new Rect()));

        return trees[team.uid];
    }

    public int count(Team team){
        return counts[team.id];
    }

    public void updateCount(Team team, int amount){
        counts[team.id] += amount;
    }

    @Override
    public void reset(){
        active.clear();
        counts = new int[Team.all().length];
        trees = new QuadTree[Team.all().length];
    }

    @Override
    public void begin(){
        for(TeamData data : Vars.state.teams.getActive()){
            if(!active.contains(data.team)){
                active.add(data.team);
            }
        }

        for(Team team : active){
            if(trees[team.uid] != null){
                trees[team.uid].clear();
            }
        }

        for(Team team : active){
            counts[team.id] = 0;
        }

        for(Unitc unit : Groups.unit){
            tree(unit.team()).insert(unit);
            counts[unit.team().id] ++;
        }
    }

    @Override
    public boolean shouldProcess(){
        return false;
    }
}
