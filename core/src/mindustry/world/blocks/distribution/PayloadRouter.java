package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
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
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        super.drawRequestRegion(req, list);

        Draw.rect(overRegion, req.drawx(), req.drawy());
    }

    public class PayloadRouterEntity extends PayloadConveyorEntity{

        @Override
        public void moved(){
            int rotations = 0;
            do{
                tile.rotation((tile.rotation() + 1) % 4);
                onProximityUpdate();
            }while((blocked || next == null) && ++rotations < 4);
        }

        @Override
        public void draw(){
            super.draw();

            Draw.z(Layer.block);

            Draw.rect(overRegion, x, y);
        }
    }
}
