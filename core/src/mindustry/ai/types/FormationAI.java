package mindustry.ai.types;

import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.ai.formations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

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
        if(leader.dead){
            unit.resetController();
            return;
        }

        unit.controlWeapons(leader.isRotate(), leader.isShooting);
        // unit.moveAt(Tmp.v1.set(deltaX, deltaY).limit(unit.type().speed));
        if(leader.isShooting){
            unit.aimLook(leader.aimX(), leader.aimY());
        }else{
            if(!leader.moving() || !unit.type().rotateShooting){
                if(unit.moving()){
                    unit.lookAt(unit.vel.angle());
                }
            }else{
                unit.lookAt(leader.rotation);
            }
        }

        Vec2 realtarget = vec.set(target);

        if(unit.isGrounded() && Vars.world.raycast(unit.tileX(), unit.tileY(), leader.tileX(), leader.tileY(), Vars.world::solid)){
            realtarget.set(Vars.pathfinder.getTargetTile(unit.tileOn(), unit.team, leader));
        }

        unit.moveAt(realtarget.sub(unit).limit(unit.type().speed));
    }

    @Override
    public void removed(Unit unit){
        if(formation != null){
            formation.removeMember(this);
        }
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
