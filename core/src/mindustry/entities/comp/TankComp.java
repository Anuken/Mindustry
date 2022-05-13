package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class TankComp implements Posc, Flyingc, Hitboxc, Unitc, ElevationMovec{
    @Import float x, y, hitSize, rotation, speedMultiplier;
    @Import boolean hovering;
    @Import UnitType type;
    @Import Team team;

    transient private float treadEffectTime, lastSlowdown = 1f;

    transient float treadTime;
    transient boolean walked;

    @Override
    public void update(){
        //dust
        if(walked && !headless && !inFogTo(player.team())){
            treadEffectTime += Time.delta;
            if(treadEffectTime >= 6f && type.treadRects.length > 0){
                //first rect should always be at the back
                var treadRect = type.treadRects[0];

                float xOffset = (-(treadRect.x + treadRect.width/2f)) / 4f;
                float yOffset = (-(treadRect.y + treadRect.height/2f)) / 4f;

                for(int i : Mathf.signs){
                    Tmp.v1.set(xOffset * i, yOffset - treadRect.height / 2f / 4f).rotate(rotation - 90);

                    //TODO could fin for a while
                    Effect.floorDustAngle(type.treadEffect, Tmp.v1.x + x, Tmp.v1.y + y, rotation + 180f);
                }

                treadEffectTime = 0f;
            }
        }

        //calculate overlapping tiles so it slows down when going "over" walls
        int r = Math.max(Math.round(hitSize * 0.6f / tilesize), 1);

        int solids = 0, total = (r*2+1)*(r*2+1);
        for(int dx = -r; dx <= r; dx++){
            for(int dy = -r; dy <= r; dy++){
                Tile t = Vars.world.tileWorld(x + dx*tilesize, y + dy*tilesize);
                if(t == null ||  t.solid()){
                    solids ++;
                }

                //TODO should this apply to the player team(s)? currently PvE due to balancing
                if(type.crushDamage > 0 && walked && t != null && t.build != null && t.build.team != team
                    //damage radius is 1 tile smaller to prevent it from just touching walls as it passes
                    && Math.max(Math.abs(dx), Math.abs(dy)) <= r - 1){

                    t.build.damage(team, type.crushDamage * Time.delta * t.block().crushDamageMultiplier);
                }
            }
        }

        lastSlowdown = Mathf.lerp(1f, type.crawlSlowdown, Mathf.clamp((float)solids / total / type.crawlSlowdownFrac));

        //trigger animation only when walking manually
        if(walked || net.client()){
            float len = deltaLen();
            treadTime += len;
            walked = false;
        }
    }

    @Override
    @Replace
    public float floorSpeedMultiplier(){
        Floor on = isFlying() || hovering ? Blocks.air.asFloor() : floorOn();
        //TODO take into account extra blocks
        return on.speedMultiplier * speedMultiplier * lastSlowdown;
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
        if(!vector.isZero(0.001f)){
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
