package mindustry.ai.types;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;

import static mindustry.Vars.*;

public class BuilderAI extends AIController{
    public static float buildRadius = 1500, retreatDst = 110f, fleeRange = 370f, retreatDelay = Time.toSeconds * 2f;

    boolean found = false;
    @Nullable Unit following;
    @Nullable Teamc enemy;
    float retreatTimer;
    @Nullable BlockPlan lastPlan;

    @Override
    public void updateMovement(){

        if(target != null && shouldShoot()){
            unit.lookAt(target);
        }

        unit.updateBuilding = true;

        if(following != null){
            retreatTimer = 0f;
            //try to follow and mimic someone

            //validate follower
            if(!following.isValid() || !following.activelyBuilding()){
                following = null;
                unit.plans.clear();
                return;
            }

            //set to follower's first build plan, whatever that is
            unit.plans.clear();
            unit.plans.addFirst(following.buildPlan());
            lastPlan = null;
        }else if(unit.buildPlan() == null){
            //not following anyone or building
            if(timer.get(timerTarget4, 40)){
                enemy = target(unit.x, unit.y, fleeRange, true, true);
            }

            //fly away from enemy when not doing anything, but only after a delay
            if((retreatTimer += Time.delta) >= retreatDelay){
                if(enemy != null){
                    var core = unit.closestCore();
                    if(core != null && !unit.within(core, retreatDst)){
                        moveTo(core, retreatDst);
                    }
                }
            }
        }

        if(unit.buildPlan() != null){
            retreatTimer = 0f;
            //approach request if building
            BuildPlan req = unit.buildPlan();

            //clear break plan if another player is breaking something.
            if(!req.breaking && timer.get(timerTarget2, 40f)){
                for(Player player : Groups.player){
                    if(player.isBuilder() && player.unit().activelyBuilding() && player.unit().buildPlan().samePos(req) && player.unit().buildPlan().breaking){
                        unit.plans.removeFirst();
                        //remove from list of plans
                        unit.team.data().blocks.remove(p -> p.x == req.x && p.y == req.y);
                        return;
                    }
                }
            }

            boolean valid =
                !(lastPlan != null && lastPlan.removed) &&
                    ((req.tile() != null && req.tile().build instanceof ConstructBuild cons && cons.current == req.block) ||
                    (req.breaking ?
                        Build.validBreak(unit.team(), req.x, req.y) :
                        Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation)));

            if(valid){
                //move toward the request
                moveTo(req.tile(), buildingRange - 20f);
            }else{
                //discard invalid request
                unit.plans.removeFirst();
                lastPlan = null;
            }
        }else{

            //follow someone and help them build
            if(timer.get(timerTarget2, 60f)){
                found = false;

                Units.nearby(unit.team, unit.x, unit.y, buildRadius, u -> {
                    if(found) return;

                    if(u.canBuild() && u != unit && u.activelyBuilding()){
                        BuildPlan plan = u.buildPlan();

                        Building build = world.build(plan.x, plan.y);
                        if(build instanceof ConstructBuild cons){
                            float dist = Math.min(cons.dst(unit) - buildingRange, 0);

                            //make sure you can reach the request in time
                            if(dist / unit.speed() < cons.buildCost * 0.9f){
                                following = u;
                                found = true;
                            }
                        }
                    }
                });
            }

            float rebuildTime = (unit.team.rules().ai ? Mathf.lerp(15f, 2f, unit.team.rules().aiTier) : 2f) * 60f;

            //find new request
            if(!unit.team.data().blocks.isEmpty() && following == null && timer.get(timerTarget3, rebuildTime)){
                Queue<BlockPlan> blocks = unit.team.data().blocks;
                BlockPlan block = blocks.first();

                //check if it's already been placed
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    blocks.removeFirst();
                }else if(Build.validPlace(content.block(block.block), unit.team(), block.x, block.y, block.rotation)){ //it's valid.
                    lastPlan = block;
                    //add build request.
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                    //shift build plan to tail so next unit builds something else.
                    blocks.addLast(blocks.removeFirst());
                }else{
                    //shift head of queue to tail, try something else next time
                    blocks.removeFirst();
                    blocks.addLast(block);
                }
            }
        }
    }

    @Override
    public AIController fallback(){
        return unit.type.flying ? new FlyingAI() : new GroundAI();
    }

    @Override
    public boolean useFallback(){
        return state.rules.waves && unit.team == state.rules.waveTeam && !unit.team.rules().ai;
    }

    @Override
    public boolean shouldShoot(){
        return !unit.isBuilding();
    }
}
