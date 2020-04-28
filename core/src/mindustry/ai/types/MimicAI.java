package mindustry.ai.types;

import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class MimicAI extends AIController{
    public @Nullable Unitc control;

    public MimicAI(@Nullable Unitc control){
        this.control = control;
    }

    public MimicAI(){
    }

    @Override
    public void update(){
        if(control != null){
            unit.controlWeapons(control.isRotate(), control.isShooting());
            //TODO this isn't accurate
            unit.moveAt(Tmp.v1.set(control.vel()).limit(unit.type().speed));
            if(control.isShooting()){
                unit.aimLook(control.aimX(), control.aimY());
            }else{
                unit.lookAt(unit.vel().angle());
            }
        }
    }
}
