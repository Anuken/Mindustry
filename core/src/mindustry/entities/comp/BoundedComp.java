package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

@Component
abstract class BoundedComp implements Velc, Posc, Healthc, Flyingc{
    static final float warpDst = 180f;

    @Import float x, y;
    @Import Vec2 vel;

    @Override
    public void update(){
        //repel unit out of bounds
        if(x < 0) vel.x += (-x/warpDst);
        if(y < 0) vel.y += (-y/warpDst);
        if(x > world.unitWidth()) vel.x -= (x - world.unitWidth())/warpDst;
        if(y > world.unitHeight()) vel.y -= (y - world.unitHeight())/warpDst;

        //clamp position if not flying
        if(isGrounded()){
            x = Mathf.clamp(x, 0, world.width() * tilesize - tilesize);
            y = Mathf.clamp(y, 0, world.height() * tilesize - tilesize);
        }

        //kill when out of bounds
        if(x < -finalWorldBounds || y < -finalWorldBounds || x >= world.width() * tilesize + finalWorldBounds || y >= world.height() * tilesize + finalWorldBounds){
            kill();
        }
    }
}
