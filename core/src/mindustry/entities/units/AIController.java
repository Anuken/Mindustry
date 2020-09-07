package mindustry.entities.units;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.indexer;

public class AIController implements UnitController{
    protected static final Vec2 vec = new Vec2();
    protected static final int timerTarget = 0;

    protected Unit unit;
    protected Teamc target;
    protected Interval timer = new Interval(4);

    {
        timer.reset(0, Mathf.random(40f));
    }

    protected void targetClosestAllyFlag(BlockFlag flag){
        Tile target = Geometry.findClosest(unit.x, unit.y, indexer.getAllied(unit.team, flag));
        if(target != null) this.target = target.build;
    }

    protected void targetClosestEnemyFlag(BlockFlag flag){
        Tile target = Geometry.findClosest(unit.x, unit.y, indexer.getEnemy(unit.team, flag));
        if(target != null) this.target = target.build;
    }

    protected boolean retarget(){
        return timer.get(timerTarget, 30);
    }

    protected void targetClosest(){
        Teamc newTarget = Units.closestTarget(unit.team, unit.x, unit.y, Math.max(unit.range(), unit.type().range), u -> (unit.type().targetAir && u.isFlying()) || (unit.type().targetGround && !u.isFlying()));
        if(newTarget != null){
            target = newTarget;
        }
    }

    protected void init(){

    }

    @Override
    public void unit(Unit unit){
        if(this.unit == unit) return;

        this.unit = unit;
        init();
    }

    @Override
    public Unit unit(){
        return unit;
    }
}
