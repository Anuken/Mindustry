package mindustry.entities.type.base;

import arc.math.Mathf;
import arc.math.geom.Geometry;
import mindustry.entities.units.*;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;

import static mindustry.Vars.*;

public abstract class BaseDrone extends FlyingUnit{
    public final UnitState retreat = new UnitState(){
        public void entered(){
            target = null;
        }

        public void update(){
            if(health >= maxHealth()){
                state.set(getStartState());
            }else if(!targetHasFlag(BlockFlag.repair)){
                if(retarget()){
                    Tile repairPoint = Geometry.findClosest(x, y, indexer.getAllied(team, BlockFlag.repair));
                    if(repairPoint != null){
                        target = repairPoint;
                    }else{
                        setState(getStartState());
                    }
                }
            }else{
                circle(40f);
            }
        }
    };

    public boolean countsAsEnemy(){
        return false;
    }

    @Override
    public void onCommand(UnitCommand command){
        //do nothing, normal commands are not applicable here
    }

    @Override
    protected void updateRotation(){
        if(target != null && shouldRotate() && target.dst(this) < type.range){
            rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.3f);
        }else{
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), 0.3f);
        }
    }

    @Override
    public void behavior(){
        if(health <= maxHealth() * type.retreatPercent && !state.is(retreat) && Geometry.findClosest(x, y, indexer.getAllied(team, BlockFlag.repair)) != null){
            setState(retreat);
        }
    }

    public boolean shouldRotate(){
        return state.is(getStartState());
    }

    @Override
    public abstract UnitState getStartState();

}
