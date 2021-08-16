package mindustry.ai.formations.patterns;

import arc.math.*;
import arc.math.geom.*;
import mindustry.ai.formations.*;

public class CircleFormation extends FormationPattern{

    @Override
    public Vec3 calculateSlotLocation(Vec3 outLocation, int slotNumber){
        if(slots > 1){
            float angle = (360f * slotNumber) / slots + (slots == 8 ? 22.5f : 0);
            float radius = spacing / (float)Math.sin(180f / slots * Mathf.degRad);
            outLocation.set(Angles.trnsx(angle, radius), Angles.trnsy(angle, radius), angle);
        }else{
            outLocation.set(0, spacing * 1.1f, 360f * slotNumber);
        }

        return outLocation;
    }

}
