package mindustry.entities.def;

import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class LegsComp implements Posc, Flyingc, Hitboxc, DrawLayerGroundUnderc, Unitc, Legsc{
    transient float x, y;

    float baseRotation, walkTime;

    @Override
    public void update(){
        if(vel().len() > 0.5f){
            baseRotation = vel().angle();
            walkTime += Time.delta()*vel().len()/1f;
        }
    }

    @Override
    public void drawGroundUnder(){
        type().drawLegs(this);
    }
}
