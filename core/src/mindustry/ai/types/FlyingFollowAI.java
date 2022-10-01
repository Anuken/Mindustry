package mindustry.ai.types;

import arc.math.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

//TODO generally strange behavior
/** AI/wave team only! This is used for wave support flyers. */
public class FlyingFollowAI extends FlyingAI{
    public Teamc following;

    @Override
    public void updateMovement(){
        unloadPayloads();

        if(following != null){
            moveTo(following, (following instanceof Sized s ? s.hitSize()/2f * 1.1f : 0f) + unit.hitSize/2f + 15f, 50f);
        }else if(target != null && unit.hasWeapons()){
            moveTo(target, 80f);
        }

        if(shouldFaceTarget()){
            unit.lookAt(target);
        }else if(following != null){
            unit.lookAt(following);
        }

        if(timer.get(timerTarget3, 30f)){
            following = Units.closest(unit.team, unit.x, unit.y, Math.max(unit.type.range, 400f), u -> !u.dead() && u.type != unit.type, (u, tx, ty) -> -u.maxHealth + Mathf.dst2(u.x, u.y, tx, ty) / 6400f);
        }
    }

    public boolean shouldFaceTarget(){
        return target != null && (following == null || unit.within(target, unit.range()));
    }

    @Override
    public void updateVisuals(){
        if(unit.isFlying()){
            unit.wobble();

            if(!shouldFaceTarget()){
                unit.lookAt(unit.prefRotation());
            }
        }
    }

    @Override
    public AIController fallback(){
        return new FlyingAI();
    }

    @Override
    public boolean useFallback(){
        //only AI teams use this controller
        return Vars.state.rules.pvp || Vars.state.rules.waveTeam != unit.team;
    }

}
