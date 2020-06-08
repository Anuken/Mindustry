package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

@Component
abstract class LegsComp implements Posc, Rotc, Hitboxc, Flyingc, Unitc, ElevationMovec{
    @Import float x, y;
    @Import UnitType type;

    transient Leg[] legs = {};
    transient float totalLength;
    transient float moveSpace;
    transient float baseRotation;

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
        Vec2 moveOffset = Tmp.v4.trns(rot, trns).add(x, y);

        for(int i = 0; i < legs.length; i++){
            float dstRot = legAngle(rot, i);
            Vec2 baseOffset = Tmp.v5.trns(dstRot, type.legBaseOffset).add(moveOffset);
            float rot2 = Angles.moveToward(dstRot, rot + (Angles.angleDist(dstRot, rot) < 90f ? 180f : 0), type.legBend * 360f / legs.length / 4f);
            Leg l = legs[i];

            float stageF = (totalLength + i*type.legPairOffset) / moveSpace;
            int stage = (int)stageF;
            int group = stage % div;
            boolean move = i % div == group;
            l.moving = move;
            l.stage = stageF % 1f;

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
                }

                l.group = group;
            }

            //leg destination
            Vec2 legDest = Tmp.v1.trns(dstRot, legLength).add(baseOffset);
            //join destination
            Vec2 jointDest = Tmp.v2.trns(rot2, legLength / 2f + type.legBaseOffset).add(moveOffset);

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
