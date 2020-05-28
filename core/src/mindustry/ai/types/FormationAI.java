package mindustry.ai.types;

import arc.math.geom.*;
import mindustry.*;
import mindustry.ai.formations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class FormationAI extends AIController implements FormationMember{
    public Unitc leader;

    private Vec3 target = new Vec3();
    private Formation formation;

    public FormationAI(Unitc leader, Formation formation){
        this.leader = leader;
        this.formation = formation;
    }

    @Override
    public void init(){
        target.set(unit.x(), unit.y(), 0);
    }

    @Override
    public void update(){
        unit.controlWeapons(leader.isRotate(), leader.isShooting());
        // unit.moveAt(Tmp.v1.set(deltaX, deltaY).limit(unit.type().speed));
        if(leader.isShooting()){
            unit.aimLook(leader.aimX(), leader.aimY());
        }else{
            if(!unit.vel().isZero(0.001f)){
                unit.lookAt(unit.vel().angle());
            }else{
                unit.lookAt(leader.rotation());
            }
        }

        Vec2 realtarget = vec.set(target);

        if(unit.isGrounded() && Vars.world.raycast(unit.tileX(), unit.tileY(), leader.tileX(), leader.tileY(), Vars.world::solid)){
            realtarget.set(Vars.pathfinder.getTargetTile(unit.tileOn(), unit.team(), leader));
        }

        unit.moveAt(realtarget.sub(unit).limit(unit.type().speed));
    }

    @Override
    public boolean isBeingControlled(Unitc player){
        return leader == player;
    }

    @Override
    public Vec3 formationPos(){
        return target;
    }
}
