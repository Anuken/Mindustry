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

@Component
abstract class LegsComp implements Posc, Rotc, Hitboxc, Flyingc, Unitc, ElevationMovec{
    @Import float x, y;
    @Import UnitType type;

    transient Leg[] legs = {};
    transient float totalLength;
    transient int lastGroup;
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
        int div = Math.max(legs.length / 2, 2);
        float moveSpace = legLength / 1.6f / (div / 2f) * type.legMoveSpace;

        totalLength += Mathf.dst(deltaX(), deltaY());


        //float movespace = 360f / legs.length / 4f;
        float stepMult = 0.8f;
        float trns = legLength/2f*stepMult * 1.3f;//Mathf.dst(deltaX(), deltaY()) * 12.5f * div/1.5f * type.legTrns;

        //trns = moveSpace * 0.7f;
        //trns = 0;

        //rotation + offset vector
        Vec2 posOffset = Tmp.v4.trns(rot, trns);
        float approach = Mathf.dst(deltaX(), deltaY());

        for(int i = 0; i < legs.length; i++){
            Leg l = legs[i];
            float dstRot = legAngle(rot, i);
            boolean side = i < legs.length/2;
            //float rot2 = Angles.moveToward(dstRot, rot + (Angles.angleDist(dstRot, rot) < 90f ? 180f : 0), movespace);

            int stage = (int)((totalLength + i*type.legPairOffset) / moveSpace);
            int group = stage % div;
            boolean move = i % div == group;

            if(l.group != group){

                //create effect when transitioning to a group it can't move in
                if(!move && i % div == l.group){
                    Floor floor = Vars.world.floorWorld(l.base.x, l.base.y);
                    if(floor.isLiquid){
                        floor.walkEffect.at(l.base.x, l.base.y, 0, floor.mapColor);
                    }else{
                        Fx.unitLandSmall.at(l.base.x, l.base.y, 0.5f, floor.mapColor);
                    }

                    //shake when legs contact ground
                    if(type.landShake > 0){
                        Effects.shake(type.landShake, type.landShake, l.base);
                    }
                }


                l.group = group;
            }

            Vec2 offset = Tmp.v5.trns(dstRot, type.legBaseOffset).add(x, y);

            //leg destination
            Vec2 footDest = Tmp.v1.trns(dstRot - Mathf.sign(i % 2 == 0) * 0, legLength*stepMult).add(offset).add(posOffset);
            //joint destination
            //Tmp.v2.trns(rot2, legLength / 2f + type.legBaseOffset).add(x, y).add(offset);


            if(move){
                l.base.lerpDelta(footDest, moveSpeed);
                //l.joint.lerpDelta(Tmp.v2, moveSpeed / 4f);
            }

            Vec2 result = Tmp.v2;
            InverseKinematics.solve(legLength/2f, legLength/2f, Tmp.v6.set(l.base).sub(offset), side, result);
            result.add(offset);

            // if()

            //l.joint.lerpDelta(Tmp.v2, moveSpeed / 4f);
            l.joint.set(result);
        }
    }

    /** @return outwards facing angle of leg at the specified index. */
    float legAngle(float rotation, int index){
        return rotation + 360f / legs.length * index + (360f / legs.length / 2f);
    }

    /*
    @Replace
    public boolean isGrounded(){
        return true;
    }

    @Replace
    public boolean isFlying(){
        return false;
    }*/
}
