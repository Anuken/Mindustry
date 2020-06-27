package mindustry.entities.comp;

import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

@Component
abstract class TrailComp implements Unitc{
    @Import UnitType type;
    @Import float x, y, rotation;

    transient Trail trail = new Trail(6);

    @Override
    public void update(){
        trail.length = type.trailLength;

        float scale = elevation();
        float offset = type.engineOffset/2f + type.engineOffset/2f*scale;

        float cx = x + Angles.trnsx(rotation + 180, offset), cy = y + Angles.trnsy(rotation + 180, offset);
        trail.update(cx, cy);
    }
}
