package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.formations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

public class FormationAI extends AIController implements FormationMember{
    public Unit leader;

    private Vec3 target = new Vec3();
    private @Nullable Formation formation;

    public FormationAI(Unit leader, Formation formation){
        this.leader = leader;
        this.formation = formation;
    }

    @Override
    public void init(){
        target.set(unit.x, unit.y, 0);
    }

    @Override
    public void updateUnit(){
        UnitType type = unit.type();

        if(leader.dead){
            unit.resetController();
            return;
        }

        if(unit.type().canBoost && unit.canPassOn()){
            unit.elevation = Mathf.approachDelta(unit.elevation, 0f, 0.08f);
        }

        unit.controlWeapons(true, leader.isShooting);
        // unit.moveAt(Tmp.v1.set(deltaX, deltaY).limit(unit.type().speed));

        unit.aim(leader.aimX(), leader.aimY());

        if(unit.type().rotateShooting){
            unit.lookAt(leader.aimX(), leader.aimY());
        }else if(unit.moving()){
            unit.lookAt(unit.vel.angle());
        }

        Vec2 realtarget = vec.set(target);

        float margin = 3f;

        if(unit.dst(realtarget) <= margin){
            unit.vel.approachDelta(Vec2.ZERO, type.speed * type.accel / 2f);
        }else{
            unit.moveAt(realtarget.sub(unit).limit(type.speed));
        }
    }

    @Override
    public void removed(Unit unit){
        if(formation != null){
            formation.removeMember(this);
            unit.resetController();
        }
    }

    @Override
    public float formationSize(){
        return unit.hitSize * 1f;
    }

    @Override
    public boolean isBeingControlled(Unit player){
        return leader == player;
    }

    @Override
    public Vec3 formationPos(){
        return target;
    }
}
