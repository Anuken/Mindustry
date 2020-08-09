package mindustry.ai.types;

import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

public class FlyingAI extends AIController{

    @Override
    public void updateMovement(){
        if(unit.moving()){
            unit.lookAt(unit.vel.angle());
        }

        if(unit.isFlying()){
            unit.wobble();
        }

        if(target != null && unit.hasWeapons()){
            if(unit.type().weapons.first().rotate){
                moveTo(unit.range() * 0.8f);
                unit.lookAt(target);
            }else{
                attack(80f);
            }
        }
    }

    @Override
    protected Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        Teamc result = target(x, y, range, air, ground);
        if(result != null) return result;

        if(ground) result = targetFlag(x, y, BlockFlag.producer, true);
        if(result != null) return result;

        if(ground) result = targetFlag(x, y, BlockFlag.turret, true);
        if(result != null) return result;

        return null;
    }

    //TODO clean up

    protected void circle(float circleLength){
        circle(circleLength, unit.type().speed);
    }

    protected void circle(float circleLength, float speed){
        if(target == null) return;

        vec.set(target).sub(unit);

        if(vec.len() < circleLength){
            vec.rotate((circleLength - vec.len()) / circleLength * 180f);
        }

        vec.setLength(speed * Time.delta);

        unit.moveAt(vec);
    }

    protected void moveTo(float circleLength){
        if(target == null) return;

        vec.set(target).sub(unit);

        float length = circleLength <= 0.001f ? 1f : Mathf.clamp((unit.dst(target) - circleLength) / 100f, -1f, 1f);

        vec.setLength(unit.type().speed * Time.delta * length);
        if(length < -0.5f){
            vec.rotate(180f);
        }else if(length < 0){
            vec.setZero();
        }

        unit.moveAt(vec);
    }

    protected void attack(float circleLength){
        vec.set(target).sub(unit);

        float ang = unit.angleTo(target);
        float diff = Angles.angleDist(ang, unit.rotation());

        if(diff > 100f && vec.len() < circleLength){
            vec.setAngle(unit.vel().angle());
        }else{
            vec.setAngle(Mathf.slerpDelta(unit.vel().angle(), vec.angle(), 0.6f));
        }

        vec.setLength(unit.type().speed * Time.delta);

        unit.moveAt(vec);
    }
}
