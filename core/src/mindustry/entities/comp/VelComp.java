package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

@Component
abstract class VelComp implements Posc{
    @Import float x, y;

    @SyncLocal Vec2 vel = new Vec2();

    transient float drag = 0f;

    //velocity needs to be called first, as it affects delta and lastPosition
    @MethodPriority(-1)
    @Override
    public void update(){
        //do not update velocity on the client at all, unless it's non-interpolated
        //velocity conflicts with interpolation.
        if(!net.client() || isLocal()){
            float px = x, py = y;
            move(vel.x * Time.delta, vel.y * Time.delta);
            if(Mathf.equal(px, x)) vel.x = 0;
            if(Mathf.equal(py, y)) vel.y = 0;

            vel.scl(Math.max(1f - drag * Time.delta, 0));
        }
    }

    /** @return function to use for check solid state. if null, no checking is done. */
    @Nullable
    SolidPred solidity(){
        return null;
    }

    /** @return whether this entity can move through a location*/
    boolean canPass(int tileX, int tileY){
        SolidPred s = solidity();
        return s == null || !s.solid(tileX, tileY);
    }

    /** @return whether this entity can exist on its current location*/
    boolean canPassOn(){
        return canPass(tileX(), tileY());
    }

    boolean moving(){
        return !vel.isZero(0.01f);
    }

    void move(Vec2 v){
        move(v.x, v.y);
    }

    void move(float cx, float cy){
        SolidPred check = solidity();

        if(check != null){
            collisions.move(self(), cx, cy, check);
        }else{
            x += cx;
            y += cy;
        }
    }

    void velAddNet(Vec2 v){
        vel.add(v);
        if(isRemote()){
            x += v.x;
            y += v.y;
        }
    }

    void velAddNet(float vx, float vy){
        vel.add(vx, vy);
        if(isRemote()){
            x += vx;
            y += vy;
        }
    }
}
