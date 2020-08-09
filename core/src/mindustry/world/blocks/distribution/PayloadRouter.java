package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.graphics.*;

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

    public class PayloadRouterEntity extends PayloadConveyorEntity{
        public float smoothRot;

        @Override
        public void add(){
            super.add();
            smoothRot = rotdeg();
        }

        @Override
        public void moved(){
            int rotations = 0;
            do{
                rotation = (rotation + 1) % 4;
                onProximityUpdate();
            }while((blocked || next == null) && ++rotations < 4);
        }

        @Override
        public void updateTile(){
            super.updateTile();

            smoothRot = Mathf.slerpDelta(smoothRot, rotdeg(), 0.2f);
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            float dst = 0.8f;

            Draw.mixcol(Pal.accent, Math.max((dst - (Math.abs(fract() - 0.5f) * 2)) / dst, 0));
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
