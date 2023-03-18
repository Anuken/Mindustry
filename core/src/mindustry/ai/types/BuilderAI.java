package mindustry.ai.types;

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
    public static float buildRadius = 1500, retreatDst = 110f, retreatDelay = Time.toSeconds * 2f;

    public @Nullable Unit assistFollowing;
    public @Nullable Unit following;
    public @Nullable Teamc enemy;
    public @Nullable BlockPlan lastPlan;

    public float fleeRange = 370f;
    public boolean alwaysFlee;
    public boolean onlyAssist;

    boolean found = false;
    float retreatTimer;

    public BuilderAI(boolean alwaysFlee, float fleeRange){
        this.alwaysFlee = alwaysFlee;
        this.fleeRange = fleeRange;
    }

    public BuilderAI(){
    }

    @Override
    public void updateMovement(){

        if(target != null && shouldShoot()){
            unit.lookAt(target);
        }

        unit.updateBuilding = true;

        if(assistFollowing != null && assistFollowing.activelyBuilding()){
            following = assistFollowing;
        }

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
        }else if(unit.buildPlan() == null || alwaysFlee){
            //not following anyone or building
            if(timer.get(timerTarget4, 40)){
                enemy = target(unit.x, unit.y, fleeRange, true, true);
            }

            //fly away from enemy when not doing anything, but only after a delay
            if((retreatTimer += Time.delta) >= retreatDelay || alwaysFlee){
                if(enemy != null){
                    unit.clearBuilding();
                    var core = unit.closestCore();
                    if(core != null && !unit.within(core, retreatDst)){
                        moveTo(core, retreatDst);
                    }
                }
            }
        }

        if(unit.buildPlan() != null){
            if(!alwaysFlee) retreatTimer = 0f;
            //approach plan if building
            BuildPlan req = unit.buildPlan();

            //clear break plan if another player is breaking something
            if(!req.breaking && timer.get(timerTarget2, 40f)){
                for(Player player : Groups.player){
                    if(player.isBuilder() && player.unit().activelyBuilding() && player.unit().buildPlan().samePos(req) && player.unit().buildPlan().breaking){
                        unit.plans.removeFirst();
                        //remove from list of plans
                        unit.team.data().plans.remove(p -> p.x == req.x && p.y == req.y);
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
                //move toward the plan
                moveTo(req.tile(), unit.type.buildRange - 20f);
            }else{
                //discard invalid plan
                unit.plans.removeFirst();
                lastPlan = null;
            }
        }else{

            if(assistFollowing != null){
                moveTo(assistFollowing, assistFollowing.type.hitSize + unit.type.hitSize/2f + 60f);
            }

            //follow someone and help them build
            if(timer.get(timerTarget2, 20f)){
                found = false;

                Units.nearby(unit.team, unit.x, unit.y, buildRadius, u -> {
                    if(found) return;

                    if(u.canBuild() && u != unit && u.activelyBuilding()){
                        BuildPlan plan = u.buildPlan();

                        Building build = world.build(plan.x, plan.y);
                        if(build instanceof ConstructBuild cons){
                            float dist = Math.min(cons.dst(unit) - unit.type.buildRange, 0);

                            //make sure you can reach the plan in time
                            if(dist / unit.speed() < cons.buildCost * 0.9f){
                                following = u;
                                found = true;
                            }
                        }
                    }
                });

                if(onlyAssist){
                    float minDst = Float.MAX_VALUE;
                    Player closest = null;
                    for(var player : Groups.player){
                        if(player.unit().canBuild() && !player.dead() && player.team() == unit.team){
                            float dst = player.dst2(unit);
                            if(dst < minDst){
                                closest = player;
                                minDst = dst;
                            }
                        }
                    }

                    assistFollowing = closest == null ? null : closest.unit();
                }
            }

            float rebuildTime = 2f * 60f;

            //find new plan
            if(!onlyAssist && !unit.team.data().plans.isEmpty() && following == null && timer.get(timerTarget3, rebuildTime)){
                Queue<BlockPlan> blocks = unit.team.data().plans;
                BlockPlan block = blocks.first();

                //check if it's already been placed
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    blocks.removeFirst();
                }else if(Build.validPlace(content.block(block.block), unit.team(), block.x, block.y, block.rotation) && (!alwaysFlee || !nearEnemy(block.x, block.y))){ //it's valid
                    lastPlan = block;
                    //add build plan
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
                    //shift build plan to tail so next unit builds something else
                    blocks.addLast(blocks.removeFirst());
                }else{
                    //shift head of queue to tail, try something else next time
                    blocks.addLast(blocks.removeFirst());
                }
            }
        }
    }

    protected boolean nearEnemy(int x, int y){
        return Units.nearEnemy(unit.team, x * tilesize - fleeRange/2f, y * tilesize - fleeRange/2f, fleeRange, fleeRange);
    }

    @Override
    public AIController fallback(){
        return unit.type.flying ? new FlyingAI() : new GroundAI();
    }

    @Override
    public boolean useFallback(){
        return state.rules.waves && unit.team == state.rules.waveTeam && !unit.team.rules().rtsAi;
    }

    @Override
    public boolean shouldShoot(){
        return !unit.isBuilding() && unit.type.canAttack;
    }
}
