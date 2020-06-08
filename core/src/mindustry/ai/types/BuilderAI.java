package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.BuildBlock.*;

import static mindustry.Vars.*;

public class BuilderAI extends AIController{

    @Override
    public void update(){
        Builderc builder = (Builderc)unit;

        if(builder.moving()){
            builder.lookAt(builder.vel().angle());
        }

        //approach request if building
        if(builder.buildRequest() != null){
            BuildRequest req = builder.buildRequest();

            boolean valid =
                (req.tile().entity instanceof BuildEntity && req.tile().<BuildEntity>ent().cblock == req.block) ||
                (req.breaking ?
                    Build.validBreak(unit.team(), req.x, req.y) :
                    Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation));

            if(valid){
                //move toward the request
                moveTo(req.tile(), buildingRange - 20f);
            }else{
                //discard invalid request
                builder.requests().removeFirst();
            }
        }else{
            //find new request
            if(!unit.team().data().blocks.isEmpty()){
                Queue<BlockPlan> blocks = unit.team().data().blocks;
                BlockPlan block = blocks.first();

                //check if it's already been placed
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block){
                    blocks.removeFirst();
                }else if(Build.validPlace(content.block(block.block), unit.team(), block.x, block.y, block.rotation)){ //it's valid.
                    //add build request.
                    BuildRequest req = new BuildRequest(block.x, block.y, block.rotation, content.block(block.block));
                    if(block.config != null){
                        req.configure(block.config);
                    }
                    builder.addBuild(req);
                }else{
                    //shift head of queue to tail, try something else next time
                    blocks.removeFirst();
                    blocks.addLast(block);
                }
            }else{
                //TODO implement AI base building
            }
        }
    }

    protected void moveTo(Position target, float circleLength){
        vec.set(target).sub(unit);

        float length = circleLength <= 0.001f ? 1f : Mathf.clamp((unit.dst(target) - circleLength) / 100f, -1f, 1f);

        vec.setLength(unit.type().speed * Time.delta() * length);
        if(length < -0.5f){
            vec.rotate(180f);
        }else if(length < 0){
            vec.setZero();
        }

        unit.moveAt(vec);
    }
}
