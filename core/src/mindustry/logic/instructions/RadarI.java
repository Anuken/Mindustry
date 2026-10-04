package mindustry.logic.instructions;

import arc.struct.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.Interval;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class RadarI implements LogicInstruction{
    public RadarTarget target1 = RadarTarget.enemy, target2 = RadarTarget.any, target3 = RadarTarget.any;
    public RadarSort sort = RadarSort.distance;
    public LogicVar radar, sortOrder, output;

    //radar instructions are special in that they cache their output and only change it at fixed intervals.
    //this prevents lag from spam of radar instructions
    public Healthc lastTarget;
    public Object lastSourceBuild;
    public Interval timer = new Interval();

    static float bestValue = 0f;
    static Unit best = null;

    public RadarI(RadarTarget target1, RadarTarget target2, RadarTarget target3, RadarSort sort, LogicVar radar, LogicVar sortOrder, LogicVar output){
        this.target1 = target1;
        this.target2 = target2;
        this.target3 = target3;
        this.sort = sort;
        this.radar = radar;
        this.sortOrder = sortOrder;
        this.output = output;
    }

    public RadarI(){
    }

    @Override
    public void run(LogicExecutor exec){
        Object base = radar.obj();

        int sortDir = sortOrder.bool() ? 1 : -1;
        LogicAI ai = null;

        if(base instanceof Ranged r && (exec.privileged || r.team() == exec.team) &&
        ((base instanceof Building b && (!b.block.privileged || exec.privileged)) || (ai = UnitControlI.checkLogicAI(exec, base, true)) != null)){ //must be a building or a controllable unit
            float range = r.range();

            Healthc targeted;

            //timers update on a fixed 30 tick interval
            //units update on a special timer per controller instance
            if((base instanceof Building && (timer.get(30f) || lastSourceBuild != base)) || (ai != null && ai.checkTargetTimer(this))){
                //if any of the targets involve enemies
                boolean enemies = target1 == RadarTarget.enemy || target2 == RadarTarget.enemy || target3 == RadarTarget.enemy;
                boolean allies = target1 == RadarTarget.ally || target2 == RadarTarget.ally || target3 == RadarTarget.ally;

                best = null;
                bestValue = 0;

                if(enemies){
                    Seq<TeamData> data = state.teams.present;
                    for(int i = 0; i < data.size; i++){
                        if(data.items[i].team != r.team()){
                            find(r, range, sortDir, data.items[i].team);
                        }
                    }
                }else if(!allies){
                    Seq<TeamData> data = state.teams.present;
                    for(int i = 0; i < data.size; i++){
                        find(r, range, sortDir, data.items[i].team);
                    }
                }else{
                    find(r, range, sortDir, r.team());
                }

                if(ai != null){
                    ai.execCache.put(this, best);
                }

                lastSourceBuild = base;
                lastTarget = targeted = best;
            }else{
                if(ai != null){
                    targeted = (Healthc)ai.execCache.get(this);
                }else{
                    targeted = lastTarget;
                }
            }

            output.setobj(targeted);
        }else{
            output.setobj(null);
        }
    }

    void find(Ranged b, float range, int sortDir, Team team){
        Units.nearby(team, b.x(), b.y(), range, u -> {
            if(!u.within(b, range) || !u.targetable(team) || b == u) return;

            boolean valid =
            target1.func.get(b.team(), u) &&
            target2.func.get(b.team(), u) &&
            target3.func.get(b.team(), u);

            if(!valid) return;

            float val = sort.func.get(b, u) * sortDir;
            if(val > bestValue || best == null){
                bestValue = val;
                best = u;
            }
        });
    }
}
