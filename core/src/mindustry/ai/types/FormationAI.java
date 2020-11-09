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
            unit.elevation = Mathf.approachDelta(unit.elevation, !unit.canPassOn() ? 1f : leader.type.canBoost ? leader.elevation : 0f, 0.08f);
        }

        unit.controlWeapons(true, leader.isShooting);
        // unit.moveAt(Tmp.v1.set(deltaX, deltaY).limit(unit.type().speed));

        unit.aim(leader.aimX(), leader.aimY());

        if(unit.type.rotateShooting){
            unit.lookAt(leader.aimX(), leader.aimY());
        }else if(unit.moving()){
            unit.lookAt(unit.vel.angle());
        }

        Vec2 realtarget = vec.set(target);

        float margin = 4f;

        float speed = unit.realSpeed();

        if(unit.dst(realtarget) <= margin){
            //unit.vel.approachDelta(Vec2.ZERO, speed * type.accel / 2f);
        }else{
            unit.moveAt(realtarget.sub(unit).limit(speed));
        }

        if(unit instanceof Minerc mine && leader instanceof Minerc com){
            if(com.mineTile() != null && mine.validMine(com.mineTile())){
                mine.mineTile(com.mineTile());

                CoreBuild core = unit.team.core();

                if(core != null && com.mineTile().drop() != null && unit.within(core, unit.type.range) && !unit.acceptsItem(com.mineTile().drop())){
                    if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
                        Call.transferItemTo(unit.stack.item, unit.stack.amount, unit.x, unit.y, core);

                        unit.clearItem();
                    }
                }
            }else{
                mine.mineTile(null);
            }
        }

        if(unit instanceof Builderc build && leader instanceof Builderc com && com.activelyBuilding()){
            build.clearBuilding();
            build.addBuild(com.buildPlan());
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
        return unit.hitSize * 1.1f;
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
