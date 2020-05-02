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

    public QuadTree<Unitc> tree(Team team){
        if(trees[team.uid] == null) trees[team.uid] = new QuadTree<>(Vars.world.getQuadBounds(new Rect()));

        return trees[team.uid];
    }

    @Override
    public void reset(){
        active.clear();
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

        for(Unitc unit : Groups.unit){
            tree(unit.team()).insert(unit);
        }
    }

    @Override
    public boolean shouldProcess(){
        return false;
    }
}
