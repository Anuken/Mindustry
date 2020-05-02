package mindustry.ai.types;

import arc.util.ArcAnnotate.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class FormationAI extends AIController{
    public @Nullable Unitc control;

    public FormationAI(@Nullable Unitc control){
        this.control = control;
    }

    public FormationAI(){
    }

    @Override
    public void update(){
        if(control != null){

            unit.controlWeapons(control.isRotate(), control.isShooting());
           // unit.moveAt(Tmp.v1.set(deltaX, deltaY).limit(unit.type().speed));
            if(control.isShooting()){
                unit.aimLook(control.aimX(), control.aimY());
            }else{
                unit.lookAt(unit.vel().angle());
            }

        }
    }

    @Override
    public boolean isFollowing(Playerc player){
        return control == player.unit();
    }
}
