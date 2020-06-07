package mindustry.entities.comp;

import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

@Component
abstract class LegsComp implements Posc, Rotc, Hitboxc, Flyingc, Unitc{
    @Import float x, y, elevation;
    @Import UnitType type;

    transient Leg[] legs = {};
    transient float totalLength;
    transient int lastGroup;
    transient float baseRotation;

    @Override
    public void update(){
        //keep elevation halfway
        elevation = 0.4f;

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

        int stage = (int)(totalLength / moveSpace);
        int group = stage % div;

        if(lastGroup != group){
            //create ripple effects when switching leg groups
            int i = 0;
            for(Leg l : legs){
                if(i++ % div == lastGroup){
                    Floor floor = Vars.world.floorWorld(l.base.x, l.base.y);
                    if(floor.isLiquid){
                        floor.walkEffect.at(l.base.x, l.base.y, 0, floor.mapColor);
                    }else{
                        Fx.unitLandSmall.at(l.base.x, l.base.y, 0.5f, floor.mapColor);
                    }

                    //shake when legs contact ground
                    if(type.landShake > 0){
                        Effects.shake(type.landShake, type.landShake, this);
                    }
                }
            }

            lastGroup = group;
        }

        float movespace = 360f / legs.length / 4f;
        float trns = vel().len() * 12.5f * div/1.5f * type.legTrns;

        //rotation + offset vector
        Tmp.v4.trns(rot, trns);

        for(int i = 0; i < legs.length; i++){
            float dstRot = legAngle(rot, i);
            float rot2 = Angles.moveToward(dstRot, rot + (Angles.angleDist(dstRot, rot) < 90f ? 180f : 0), movespace);

            Leg l = legs[i];

            //leg destination
            Tmp.v1.trns(dstRot, legLength + type.legBaseOffset).add(x, y).add(Tmp.v4);
            //join destination
            Tmp.v2.trns(rot2, legLength / 2f + type.legBaseOffset).add(x, y).add(Tmp.v4);

            if(i % div == group){
                l.base.lerpDelta(Tmp.v1, moveSpeed);
                l.joint.lerpDelta(Tmp.v2, moveSpeed / 4f);
            }

            l.joint.lerpDelta(Tmp.v2, moveSpeed / 4f);
        }
    }

    /** @return outwards facing angle of leg at the specified index. */
    float legAngle(float rotation, int index){
        return rotation + 360f / legs.length * index + (360f / legs.length / 2f);
    }

    @Override
    public void add(){
        elevation = 0.4f;
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
