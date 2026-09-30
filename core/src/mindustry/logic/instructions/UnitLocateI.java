package mindustry.logic.instructions;

import arc.math.geom.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Uses a unit to find something that may not be in its range. */
public class UnitLocateI implements LInstruction{
    public LLocate locate = LLocate.building;
    public BlockFlag flag = BlockFlag.core;
    public LVar enemy, ore;
    public LVar outX, outY, outFound, outBuild;

    public UnitLocateI(LLocate locate, BlockFlag flag, LVar enemy, LVar ore, LVar outX, LVar outY, LVar outFound, LVar outBuild){
        this.locate = locate;
        this.flag = flag;
        this.enemy = enemy;
        this.ore = ore;
        this.outX = outX;
        this.outY = outY;
        this.outFound = outFound;
        this.outBuild = outBuild;
    }

    public UnitLocateI(){
    }

    @Override
    public void run(LExecutor exec){
        if(!exec.privileged && !state.rules.logicUnitControl) return;

        Object unitObj = exec.unit.obj();
        LogicAI ai = UnitControlI.checkLogicAI(exec, unitObj, true);

        if(unitObj instanceof Unit unit && ai != null){
            ai.controlTimer = LogicAI.logicControlTimeout;

            Cache cache = (Cache)ai.execCache.get(this, Cache::new);

            if(ai.checkTargetTimer(this)){
                Tile res = null;
                boolean build = false;

                switch(locate){
                    case ore -> {
                        if(ore.obj() instanceof Item item){
                            res = state.indexer.findClosestOre(unit, item);
                        }
                    }
                    case building -> {
                        Building b = Geometry.findClosest(unit.x, unit.y, enemy.bool() ? state.indexer.getEnemy(unit.team, flag) : state.indexer.getFlagged(unit.team, flag));
                        res = b == null ? null : b.tile;
                        build = true;
                    }
                    case spawn -> {
                        res = Geometry.findClosest(unit.x, unit.y, Vars.state.spawner.getSpawns());
                    }
                    case damaged -> {
                        Building b = Units.findDamagedTile(unit.team, unit.x, unit.y);
                        res = b == null ? null : b.tile;
                        build = true;
                    }
                }

                if(res != null && (!build || res.build != null)){
                    cache.found = true;
                    //set result if found
                    outX.setnum(cache.x = World.conv(build ? res.build.x : res.worldx()));
                    outY.setnum(cache.y = World.conv(build ? res.build.y : res.worldy()));
                    outFound.setnum(1);
                }else{
                    cache.found = false;
                    outFound.setnum(0);
                }

                if(res != null && res.build != null &&
                (unit.within(res.build.x, res.build.y, Math.max(unit.range(), buildingRange)) || res.build.team == exec.team)){
                    cache.build = res.build;
                    outBuild.setobj(res.build);
                }else{
                    outBuild.setobj(null);
                }
            }else{
                outBuild.setobj(cache.build);
                outFound.setbool(cache.found);
                outX.setnum(cache.x);
                outY.setnum(cache.y);
            }
        }else{
            outFound.setbool(false);
        }
    }

    static class Cache{
        float x, y;
        boolean found;
        Building build;
    }
}
