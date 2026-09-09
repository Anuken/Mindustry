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
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class TankComp implements Posc, Hitboxc, Unitc, ElevationMovec{
    @Import float x, y, hitSize, rotation, speedMultiplier;
    @Import boolean disarmed;
    @Import UnitType type;
    @Import Team team;

    transient private float treadEffectTime, lastSlowdown = 1f;

    transient float treadTime;
    transient boolean walked;
    transient Floor lastDeepFloor;

    @Override
    public void update(){
        //dust
        if((walked || (net.client() && deltaLen() >= 0.01f)) && !headless && !inFogTo(player.team())){
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

            control.sound.loop(type.tankMoveSound, this, type.tankMoveVolume);
        }

        lastDeepFloor = null;
        boolean anyNonDeep = false;

        if(type.crushFragile && !disarmed){
            for(int i = 0; i < 8; i++){
                Point2 offset = Geometry.d8[i];
                var other = Vars.world.buildWorld(x + offset.x * tilesize, y + offset.y * tilesize);
                if(other != null && other.team != team && other.block.crushFragile){
                    other.damage(team, 999999999f);
                }
            }
        }

        //calculate overlapping tiles so it slows down when going "over" walls
        int r = Math.max((int)(hitSize * 0.75f / tilesize), 0);

        int solids = 0, total = (r*2+1)*(r*2+1);
        for(int dx = -r; dx <= r; dx++){
            for(int dy = -r; dy <= r; dy++){
                Tile t = Vars.world.tileWorld(x + dx*tilesize, y + dy*tilesize);
                if(t == null || t.solid()){
                    solids ++;
                }

                if(t != null && t.floor().isDeep()){
                    lastDeepFloor = t.floor();
                }else{
                    anyNonDeep = true;
                }

                if(type.crushDamage > 0 && !disarmed && (walked || deltaLen() >= 0.01f) && t != null
                    //damage radius is 1 tile smaller to prevent it from just touching walls as it passes
                    && Math.max(Math.abs(dx), Math.abs(dy)) <= r - 1){

                    if(t.build != null && t.build.team != team){
                        t.build.damage(team, type.crushDamage * Time.delta * t.block().crushDamageMultiplier * state.rules.unitDamage(team)
                                * ((speedMultiplier- 1) / 5 + 1));
                    }else if(t.block().unitMoveBreakable){
                        ConstructBlock.deconstructFinish(t, t.block(), self());
                    }
                }
            }
        }

        if(anyNonDeep){
            lastDeepFloor = null;
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
        Floor on = isFlying() || type.hovering ? Blocks.air.asFloor() : floorOn();
        //TODO take into account extra blocks
        return (float)Math.pow(on.speedMultiplier, type.floorMultiplier) * speedMultiplier * lastSlowdown;
    }

    @Replace
    @Override
    public @Nullable Floor drownFloor(){
        return canDrown() ? lastDeepFloor : null;
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
