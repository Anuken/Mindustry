package mindustry.ai.types;

import arc.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import mindustry.ai.formations.*;
import mindustry.ai.formations.patterns.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class FormationAI extends AIController implements FormationMember{
    public @Nullable Unitc leader;

    private transient Vec3 target = new Vec3();

    public FormationAI(@Nullable Unitc leader){
        this.leader = leader;
    }

    static Formation formation;
    static Vec2 vec = new Vec2();

    @Override
    public void init(){
        if(formation == null){
            Vec3 vec = new Vec3();

            formation = new Formation(vec, new SquareFormation());
            Core.app.addListener(new ApplicationListener(){
                @Override
                public void update(){
                    formation.updateSlots();
                    vec.set(leader.x(), leader.y(), leader.rotation());
                }
            });
        }

        formation.addMember(this);
    }

    @Override
    public void update(){
        if(leader != null){

            unit.controlWeapons(leader.isRotate(), leader.isShooting());
            // unit.moveAt(Tmp.v1.set(deltaX, deltaY).limit(unit.type().speed));
            if(leader.isShooting()){
                unit.aimLook(leader.aimX(), leader.aimY());
            }else{

                unit.lookAt(leader.rotation());
                if(!unit.vel().isZero(0.001f)){
                //    unit.lookAt(unit.vel().angle());
                }
            }



            unit.moveAt(vec.set(target).sub(unit).limit2(unit.type().speed));
        }
    }

    @Override
    public boolean isFollowing(Playerc player){
        return leader == player.unit();
    }

    @Override
    public Vec3 formationPos(){
        return target;
    }
}
