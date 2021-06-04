package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.formations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.blocks.storage.CoreBlock.*;

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

        if(leader == null || leader.dead){
            unit.resetController();
            return;
        }

        if(unit.type.canBoost){
            unit.elevation = Mathf.approachDelta(unit.elevation,
                unit.onSolid() ? 1f : //definitely cannot land
                unit.isFlying() && !unit.canLand() ? unit.elevation : //try to maintain altitude
                leader.type.canBoost ? leader.elevation : //follow leader
                0f,
            unit.type.riseSpeed);
        }

        unit.controlWeapons(true, leader.isShooting);

        unit.aim(leader.aimX(), leader.aimY());

        if(unit.type.rotateShooting){
            unit.lookAt(leader.aimX(), leader.aimY());
        }else if(unit.moving()){
            unit.lookAt(unit.vel.angle());
        }

        Vec2 realtarget = vec.set(target).add(leader.vel);

        float speed = unit.realSpeed() * Time.delta;
        unit.approach(Mathf.arrive(unit.x, unit.y, realtarget.x, realtarget.y, unit.vel, speed, 0f, speed, 1f).scl(1f / Time.delta));

        if(unit.canMine() && leader.canMine()){
            if(leader.mineTile != null && unit.validMine(leader.mineTile)){
                unit.mineTile(leader.mineTile);

                CoreBuild core = unit.team.core();

                if(core != null && leader.mineTile.drop() != null && unit.within(core, unit.type.range) && !unit.acceptsItem(leader.mineTile.drop())){
                    if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
                        Call.transferItemTo(unit, unit.stack.item, unit.stack.amount, unit.x, unit.y, core);

                        unit.clearItem();
                    }
                }
            }else{
                unit.mineTile(null);
            }
        }

        if(unit.canBuild() && leader.canBuild() && leader.activelyBuilding()){
            unit.clearBuilding();
            unit.addBuild(leader.buildPlan());
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
        return unit.hitSize * 1.3f;
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
