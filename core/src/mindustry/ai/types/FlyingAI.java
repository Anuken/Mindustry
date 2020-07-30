package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.world.meta.*;

public class FlyingAI extends AIController{

    @Override
    public void updateUnit(){
        if(unit.moving()){
            unit.rotation(unit.vel().angle());
        }

        if(unit.isFlying()){
            unit.wobble();
        }

        if(Units.invalidateTarget(target, unit.team(), unit.x(), unit.y())){
            target = null;
        }

        if(retarget()){
            targetClosest();

            if(target == null) targetClosestEnemyFlag(BlockFlag.producer);
            if(target == null) targetClosestEnemyFlag(BlockFlag.turret);
        }

        boolean shoot = false;

        if(target != null && unit.hasWeapons()){
            if(unit.type().weapons.first().rotate){
                moveTo(unit.range() * 0.85f);
                unit.lookAt(target);
            }else{
                attack(80f);
            }

            shoot = unit.inRange(target);

            if(shoot && unit.type().hasWeapons()){
                Vec2 to = Predict.intercept(unit, target, unit.type().weapons.first().bullet.speed);
                unit.aim(to);
            }
        }

        unit.controlWeapons(shoot, shoot);
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
