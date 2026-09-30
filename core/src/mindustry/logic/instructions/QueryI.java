package mindustry.logic.instructions;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class QueryI implements LInstruction{
    private static Seq<Object> paramSeq;
    private static Team paramTeam;
    private static final Cons<Bullet> bulletCons = o -> {
        if(o.team == paramTeam){
            paramSeq.add(o);
        }
    };

    public QueryShape shape = QueryShape.rect;
    public QueryType type = QueryType.unit;
    public LVar team, x, y, width, height;

    public QueryI(QueryShape shape, QueryType type, LVar team, LVar x, LVar y, LVar width, LVar height){
        this.shape = shape;
        this.type = type;
        this.team = team;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public QueryI(){
    }

    @Override
    public void run(LExecutor exec){
        if(exec.queryResult == null) return;
        Seq<Object> results = exec.queryResult.obj() instanceof Seq s ? s : null;
        if(results == null){
            results = new Seq<>(false);
            exec.queryResult.setconst(results);
        }

        float x = this.x.numfWorld(), y = this.y.numfWorld(), w = this.width.numfWorld(), h = this.height.numfWorld();
        float radius = w, circleX = x, circleY = y;
        boolean circle = shape == QueryShape.circle;
        if(circle){
            x -= radius;
            y -= radius;
            w = radius * 2f;
            h = radius * 2f;
        }

        results.clear();
        Team team = this.team.team();

        if(type == QueryType.bullet) return; //TODO: bugged due to bullets being pooled, need a way to have references to them cleaned up after death.

        switch(type){
            case unit -> {
                if(team != null){
                    team.data().tree().intersect(x, y, w, h, results.as());
                }else{
                    for(var other : state.teams.present){
                        other.tree().intersect(x, y, w, h, results.as());
                    }
                }
            }
            case bullet -> {
                if(team != null){
                    paramTeam = team;
                    paramSeq = results;
                    ((QuadTree<Bullet>)state.entities.bullet.tree()).intersect(x, y, w, h, bulletCons);
                }else{
                    state.entities.bullet.tree().intersect(x, y, w, h, results.as());
                }
            }
            case building -> {
                if(team != null){
                    if(team.data().buildingTree == null) return;
                    team.data().buildingTree.intersect(x, y, w, h, results.as());
                }else{
                    for(var other : state.teams.present){
                        if(other.buildingTree != null){
                            other.buildingTree.intersect(x, y, w, h, results.as());
                        }
                    }
                }
            }
        }

        if(circle){
            switch(type){
                //TODO: lambda capture causes garbage?
                case unit -> results.<Unit>as().removeAll(u -> !u.within(circleX, circleY, radius + u.hitSize / 2f));
                case bullet -> results.<Bullet>as().removeAll(u -> !u.within(circleX, circleY, radius + u.hitSize / 2f));
                case building -> results.<Building>as().removeAll(u -> !u.within(circleX, circleY, radius + u.hitSize() / 2f));
            }
        }
    }
}
