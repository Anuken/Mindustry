package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class LegsComp implements Posc, Rotc, Hitboxc, Flyingc, Unitc{
    @Import float x, y;
    @Import UnitType type;

    transient Leg[] legs = {};
    transient float totalLength;
    transient float moveSpace;
    transient float baseRotation;

    @Replace
    @Override
    public void move(float cx, float cy){
        collisions.moveCheck(this, cx, cy, !type.allowLegStep ? EntityCollisions::solid : EntityCollisions::legsSolid);
    }

    @Override
    public void update(){
        if(Mathf.dst(deltaX(), deltaY()) > 0.001f){
            baseRotation = Mathf.slerpDelta(baseRotation, Mathf.angle(deltaX(), deltaY()), 0.1f);
        }

        float rot = baseRotation;
        int count = type.legCount;
        float legLength = type.legLength;

        //set up initial leg positions
        if(legs.length != type.legCount){
            this.legs = new Leg[count];

            float spacing = 360f / count;

            for(int i = 0; i < legs.length; i++){
                Leg l = new Leg();

                l.joint.trns(i * spacing + rot, legLength/2f + type.legBaseOffset).add(x, y);
                l.base.trns(i * spacing + rot, legLength + type.legBaseOffset).add(x, y);

                legs[i] = l;
            }
        }

        float moveSpeed = type.legSpeed;
        int div = Math.max(legs.length / type.legGroupSize, 2);
        moveSpace = legLength / 1.6f / (div / 2f) * type.legMoveSpace;
        totalLength += Mathf.dst(deltaX(), deltaY());

        float trns = moveSpace * 0.85f * type.legTrns;

        //rotation + offset vector
        Vec2 moveOffset = Tmp.v4.trns(rot, trns);
        boolean moving = moving();

        for(int i = 0; i < legs.length; i++){
            float dstRot = legAngle(rot, i);
            Vec2 baseOffset = Tmp.v5.trns(dstRot, type.legBaseOffset).add(x, y);
            Leg l = legs[i];

            l.joint.sub(baseOffset).limit(type.maxStretch * legLength/2f).add(baseOffset);
            l.base.sub(baseOffset).limit(type.maxStretch * legLength).add(baseOffset);

            float stageF = (totalLength + i*type.legPairOffset) / moveSpace;
            int stage = (int)stageF;
            int group = stage % div;
            boolean move = i % div == group;
            boolean side = i < legs.length/2;
            //back legs have reversed directions
            boolean backLeg = Math.abs((i + 0.5f) - legs.length/2f) <= 0.501f;
            if(backLeg && type.flipBackLegs) side = !side;

            l.moving = move;
            l.stage = moving ? stageF % 1f : Mathf.lerpDelta(l.stage, 0f, 0.1f);

            if(l.group != group){

                //create effect when transitioning to a group it can't move in
                if(!move && i % div == l.group){
                    Floor floor = Vars.world.floorWorld(l.base.x, l.base.y);
                    if(floor.isLiquid){
                        floor.walkEffect.at(l.base.x, l.base.y, type.rippleScale, floor.mapColor);
                    }else{
                        Fx.unitLandSmall.at(l.base.x, l.base.y, type.rippleScale, floor.mapColor);
                    }

                    //shake when legs contact ground
                    if(type.landShake > 0){
                        Effects.shake(type.landShake, type.landShake, l.base);
                    }

                    if(type.legSplashDamage > 0){
                        Damage.damage(team(), l.base.x, l.base.y, type.legSplashRange, type.legSplashDamage, false, true);
                    }
                }

                l.group = group;
            }

            //leg destination
            Vec2 legDest = Tmp.v1.trns(dstRot, legLength * type.legLengthScl).add(baseOffset).add(moveOffset);
            //join destination
            Vec2 jointDest = Tmp.v2;//.trns(rot2, legLength / 2f + type.legBaseOffset).add(moveOffset);
            InverseKinematics.solve(legLength/2f, legLength/2f, Tmp.v6.set(l.base).sub(baseOffset), side, jointDest);
            jointDest.add(baseOffset);
            //lerp between kinematic and linear
            jointDest.lerp(Tmp.v6.set(baseOffset).lerp(l.base, 0.5f), 1f - type.kinematicScl);

            if(move){
                float moveFract = stageF % 1f;

                l.base.lerpDelta(legDest, moveFract);
                l.joint.lerpDelta(jointDest, moveFract / 2f);
            }

            l.joint.lerpDelta(jointDest, moveSpeed / 4f);
        }
    }

    /** @return outwards facing angle of leg at the specified index. */
    float legAngle(float rotation, int index){
        return rotation + 360f / legs.length * index + (360f / legs.length / 2f);
    }

}
