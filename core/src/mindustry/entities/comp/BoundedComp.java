package mindustry.entities.comp;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

@Component
abstract class BoundedComp implements Velc, Posc, Healthc, Flyingc{
    static final float warpDst = 30f;

    @Import float x, y;

    @Override
    public void update(){
        if(!net.client() || isLocal()){

            float dx = 0f, dy = 0f;

            //repel unit out of bounds
            if(x < 0) dx += (-x/warpDst);
            if(y < 0) dy += (-y/warpDst);
            if(x > world.unitWidth()) dx -= (x - world.unitWidth())/warpDst;
            if(y > world.unitHeight()) dy -= (y - world.unitHeight())/warpDst;

            velAddNet(dx * Time.delta, dy * Time.delta);
        }

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
