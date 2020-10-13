package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.*;

public class PayloadCoveredConveyor extends PayloadConveyor{
    public @Load("@-top") TextureRegion topRegion;

    public PayloadCoveredConveyor(String name){
        super(name);

        size = 3;
        rotate = true;
        update = true;
        outputsPayload = true;
        noUpdateDisabled = true;
        sync = true;
    }

    public class PayloadCoveredConveyorBuild extends PayloadConveyorBuild{

        @Override
        public void draw(){
            super.draw();
            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y, rotdeg());
        }

        @Override
        public void unitOn(Unit unit){}
    }
}
