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
    float buildRadius = 1500;
    boolean found = false;
    @Nullable Builderc following;

    @Override
    public void updateMovement(){
        Builderc builder = (Builderc)unit;

        if(builder.moving()){
            builder.lookAt(builder.vel().angle());
        }

        if(target != null && shouldShoot()){
            unit.lookAt(target);
        }

        builder.updateBuilding(true);

        if(following != null){
            //try to follow and mimic someone

            //validate follower
            if(!following.isValid() || !following.activelyBuilding()){
                following = null;
                builder.plans().clear();
                return;
            }

            //set to follower's first build plan, whatever that is
            builder.plans().clear();
            builder.plans().addFirst(following.buildPlan());
        }

        if(builder.buildPlan() != null){
            //approach request if building
            BuildPlan req = builder.buildPlan();

            boolean valid =
                (req.tile().build instanceof ConstructBuild && req.tile().<ConstructBuild>bc().cblock == req.block) ||
                (req.breaking ?
                    Build.validBreak(unit.team(), req.x, req.y) :
                    Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation));

            if(valid){
                //move toward the request
                moveTo(req.tile(), buildingRange - 20f);
            }else{
                //discard invalid request
                builder.plans().removeFirst();
            }
        }else{

            //follow someone and help them build
            if(timer.get(timerTarget2, 60f)){
                found = false;

                Units.nearby(unit.team, unit.x, unit.y, buildRadius, u -> {
                    if(found) return;

                    if(u instanceof Builderc b && u != unit && ((Builderc)u).activelyBuilding()){
                        BuildPlan plan = b.buildPlan();

                        Building build = world.build(plan.x, plan.y);
                        if(build instanceof ConstructBuild cons){
                            float dist = Math.min(cons.dst(unit) - buildingRange, 0);

                            //make sure you can reach the request in time
                            if(dist / unit.type.speed < cons.buildCost * 0.9f){
                                following = b;
                                found = true;
                            }
                        }
                    }
                });
            }

            //find new request
            if(!unit.team.data().blocks.isEmpty() && following == null && timer.get(timerTarget3, 60 * 2f)){
                Queue<BlockPlan> blocks = unit.team.data().blocks;
                BlockPlan block = blocks.first();

                //check if it's already been placed
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    blocks.removeFirst();
                }else if(Build.validPlace(content.block(block.block), unit.team(), block.x, block.y, block.rotation)){ //it's valid.
                    //add build request.
                    builder.addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
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
        return !((Builderc)unit).isBuilding();
    }
}
