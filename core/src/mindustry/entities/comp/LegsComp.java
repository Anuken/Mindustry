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

    @Override
    public void update(){
        //keep elevation halfway
        elevation = 0.5f;

        int count = type.legCount;
        float legLength = type.legLength;
        float rotation = vel().angle();

        //set up initial leg positions
        if(legs.length != type.legCount){
            this.legs = new Leg[count];

            float spacing = 360f / count;

            for(int i = 0; i < legs.length; i++){
                Leg l = new Leg();

                l.joint.trns(i * spacing + rotation, legLength/2f).add(x, y);
                l.base.trns(i * spacing + rotation, legLength).add(x, y);

                legs[i] = l;
            }
        }

        float moveSpeed = type.legSpeed;
        int div = Math.max(legs.length / 2, 2);
        float moveSpace = legLength / 1.6f / (div / 2f);

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

        Tmp.v4.trns(rotation, trns);

        for(int i = 0; i < legs.length; i++){
            float dstRot = rotation + 360f / legs.length * i + (360f / legs.length / 2f);
            float rot2 = Angles.moveToward(dstRot, rotation + (Angles.angleDist(dstRot, rotation) < 90f ? 180f : 0), movespace);

            Leg l = legs[i];

            Tmp.v1.trns(dstRot, legLength).add(x, y).add(Tmp.v4);
            Tmp.v2.trns(rot2, legLength / 2f).add(x, y).add(Tmp.v4);

            if(i % div == group){
                l.base.lerpDelta(Tmp.v1, moveSpeed);
                l.joint.lerpDelta(Tmp.v2, moveSpeed / 4f);
            }

            l.joint.lerpDelta(Tmp.v2, moveSpeed / 4f);
        }
    }

    @Override
    public void add(){
        elevation = 0.5f;
    }
}
