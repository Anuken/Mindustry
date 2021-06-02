package mindustry.ai.formations.patterns;

import arc.math.*;
import arc.math.geom.*;
import mindustry.ai.formations.*;

public class CircleFormation extends FormationPattern{
    /** Angle offset. */
    public float angleOffset = 0;

    @Override
    public Vec3 calculateSlotLocation(Vec3 outLocation, int slotNumber){
        if(slots > 1){
            float angle = (360f * slotNumber) / slots;
            float radius = spacing / (float)Math.sin(180f / slots * Mathf.degRad);
            outLocation.set(Angles.trnsx(angle, radius), Angles.trnsy(angle, radius), angle);
        }else{
            outLocation.set(0, spacing * 1.1f, 360f * slotNumber);
        }

        outLocation.z += angleOffset;

        return outLocation;
    }

}
