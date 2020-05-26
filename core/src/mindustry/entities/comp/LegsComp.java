package mindustry.entities.comp;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;

@Component
abstract class LegsComp implements Posc, Rotc, Hitboxc, Flyingc, Unitc{
    @Import float x, y, rotation, elevation;

    transient Leg[] legs = {};
    transient float totalLength;

    @Override
    public void update(){
        int count = type().legCount;
        float legLength = type().legLength;

        if(legs.length != type().legCount){
            this.legs = new Leg[count];

            float spacing = 360f / count;

            for(int i = 0; i < legs.length; i++){
                Leg l = new Leg();

                l.joint.trns(i * spacing + rotation, legLength/2f).add(x, y);
                l.base.trns(i * spacing + rotation, legLength).add(x, y);

                legs[i] = l;
            }
        }

        float moveSpeed = 0.1f;

        int div = Math.max(legs.length / 3, 2);

        float moveSpace = legLength / 1.6f / (div / 2f);

        elevation = 0.5f;

        totalLength += Mathf.dst(deltaX(), deltaY());

        int stage = (int)(totalLength / moveSpace);
        int odd = stage % div;
        float movespace = 360f / legs.length / 4f;
        float trns = vel().len() * 12.5f * div/2f;

        Tmp.v4.trns(rotation, trns);

        for(int i = 0; i < legs.length; i++){
            float dstRot = rotation + 360f / legs.length * i + (360f / legs.length / 2f);
            float rot2 = Angles.moveToward(dstRot, rotation + (Angles.angleDist(dstRot, rotation) < 90f ? 180f : 0), movespace);

            //float ox = Mathf.randomSeedRange(i, 6f), oy = Mathf.randomSeedRange(i, 6f);

            Leg l = legs[i];

            //Tmp.v3.trns(Mathf.randomSeed(stage + i*3, 360f), 10f);
            Tmp.v3.setZero();

            Tmp.v1.trns(dstRot, legLength).add(x, y).add(Tmp.v3).add(Tmp.v4);
            Tmp.v2.trns(rot2, legLength / 2f).add(x, y).add(Tmp.v3).add(Tmp.v4);

            if(i % div == odd){
                l.base.lerpDelta(Tmp.v1, moveSpeed);
                l.joint.lerpDelta(Tmp.v2, moveSpeed / 4f);
            }

            l.joint.lerpDelta(Tmp.v2, moveSpeed / 4f);
        }
    }
}
