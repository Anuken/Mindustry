package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class TankComp implements Posc, Flyingc, Hitboxc, Unitc, ElevationMovec{
    @Import float x, y, hitSize, rotation;
    @Import UnitType type;

    transient private float treadEffectTime;

    transient float treadTime;
    transient boolean walked;

    @Override
    public void update(){
        //dust
        if(walked && !headless){
            treadEffectTime += Time.delta;
            if(treadEffectTime >= 6f){
                var treadRegion = type.treadRegion;
                var treadRect = type.treadRect;

                float xOffset = (treadRegion.width/2f - (treadRect.x + treadRect.width/2f)) / 4f;
                float yOffset = (treadRegion.height/2f - (treadRect.y + treadRect.height/2f)) / 4f;

                for(int i : Mathf.signs){
                    Tmp.v1.set(xOffset * i, yOffset - treadRect.height / 2f / 4f - 2f).rotate(rotation - 90);

                    Effect.floorDustAngle(type.treadEffect, Tmp.v1.x + x, Tmp.v1.y + y, rotation + 180f);
                }

                treadEffectTime = 0f;
            }
        }

        //trigger animation only when walking manually
        if(walked || net.client()){
            float len = deltaLen();
            treadTime += len;
            walked = false;
        }
    }

    @Replace
    @Override
    public @Nullable Floor drownFloor(){
        //tanks can only drown when all the nearby floors are deep
        //TODO implement properly
        if(hitSize >= 12 && canDrown()){
            for(Point2 p : Geometry.d8){
                Floor f = world.floorWorld(x + p.x * tilesize, y + p.y * tilesize);
                if(!f.isDeep()){
                    return null;
                }
            }
        }
        return canDrown() ? floorOn() : null;
    }

    @Override
    public void moveAt(Vec2 vector, float acceleration){
        //mark walking state when moving in a controlled manner
        if(!vector.isZero()){
            walked = true;
        }
    }

    @Override
    public void approach(Vec2 vector){
        //mark walking state when moving in a controlled manner
        if(!vector.isZero(0.001f)){
            walked = true;
        }
    }
}
