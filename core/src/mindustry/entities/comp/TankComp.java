package mindustry.entities.comp;

import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class TankComp implements Posc, Flyingc, Hitboxc, Unitc, ElevationMovec{
    @Import float x, y, hitSize;
    @Import UnitType type;

    transient float treadTime;
    transient boolean walked;

    @Override
    public void update(){
        //trigger animation only when walking manually
        if(walked || net.client()){
            float len = deltaLen();
            treadTime += len;
            walked = false;
        }

        //TODO treads should create dust, see MechComp
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
