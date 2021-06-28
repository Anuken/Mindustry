package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.blocks.payloads.*;

public class PayloadRouter extends PayloadConveyor{
    public @Load("@-over") TextureRegion overRegion;

    public PayloadRouter(String name){
        super(name);

        outputsPayload = true;
        outputFacing = false;
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        super.drawRequestRegion(req, list);

        Draw.rect(overRegion, req.drawx(), req.drawy());
    }

    public class PayloadRouterBuild extends PayloadConveyorBuild{
        public float smoothRot;
        public float controlTime = -1f;

        @Override
        public void add(){
            super.add();
            smoothRot = rotdeg();
        }

        public void pickNext(){
            if(item != null && controlTime <= 0f){
                int rotations = 0;
                do{
                    rotation = (rotation + 1) % 4;
                    onProximityUpdate();
                    //force update to transfer if necessary
                    if(next instanceof PayloadConveyorBuild && !(next instanceof PayloadRouterBuild)){
                        next.updateTile();
                    }
                    //this condition intentionally uses "accept from itself" conditions, because payload conveyors only accept during the start
                    //"accept from self" conditions are for dropped payloads and are less restrictive
                }while((blocked || next == null || !next.acceptPayload(next, item)) && ++rotations < 4);
            }
        }

        @Override
        public void control(LAccess type, double p1, double p2, double p3, double p4){
            super.control(type, p1, p2, p3, p4);
            if(type == LAccess.config){
                rotation = (int)p1;
                //when manually controlled, routers do not turn automatically for a while, same as turrets
                controlTime = Building.timeToUncontrol;
            }
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            super.handlePayload(source, payload);
            pickNext();
        }

        @Override
        public void moveFailed(){
            pickNext();
        }

        @Override
        public void updateTile(){
            super.updateTile();

            controlTime -= Time.delta;
            smoothRot = Mathf.slerpDelta(smoothRot, rotdeg(), 0.2f);
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            float dst = 0.8f;

            Draw.mixcol(team.color, Math.max((dst - (Math.abs(fract() - 0.5f) * 2)) / dst, 0));
            Draw.rect(topRegion, x, y, smoothRot);
            Draw.reset();

            Draw.rect(overRegion, x, y);

            Draw.z(Layer.blockOver);

            if(item != null){
                item.draw();
            }
        }
    }
}
