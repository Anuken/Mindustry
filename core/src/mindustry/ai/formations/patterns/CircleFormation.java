package mindustry.ai.formations.patterns;

import arc.math.*;
import arc.math.geom.*;
import mindustry.ai.formations.*;

public class CircleFormation extends FormationPattern{
    /** The radius of one member. This is needed to determine how close we can pack a given number of members around circle. */
    public float memberRadius;
    /** Angle offset. */
    public float angleOffset = 0;

    public CircleFormation(float memberRadius){
        this.memberRadius = memberRadius;
    }

    @Override
    public Vec3 calculateSlotLocation(Vec3 outLocation, int slotNumber){
        if(slots > 1){
            float angle = (360f * slotNumber) / slots;
            float radius = memberRadius / (float)Math.sin(180f / slots * Mathf.degRad);
            outLocation.set(Angles.trnsx(angle, radius), Angles.trnsy(angle, radius), angle);
        }else{
            outLocation.set(0, 0, 360f * slotNumber);
        }

        outLocation.z += angleOffset;

        return outLocation;
    }

}
