package mindustry.world.blocks.distribution;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;
import mindustry.world.blocks.distribution.*;

import static mindustry.Vars.*;

public class CoveredConveyor extends Conveyor implements Autotiler{
    public @Load(value = "@-top-#1", lengths = {7}) TextureRegion[] topRegions;

    protected CoveredConveyor(String name){
        super(name);
        rotate = true;
        update = true;
        group = BlockGroup.transportation;
        hasItems = true;
        itemCapacity = 4;
        conveyorPlacement = true;

        idleSound = Sounds.conveyor;
        idleSoundVolume = 0.004f;
        unloadable = false;
        noUpdateDisabled = false;
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        TextureRegion region = regions[bits[0]][0];
        TextureRegion topRegion = topRegions[bits[0]];
        Draw.rect(region, req.drawx(), req.drawy(), region.width * bits[1] * Draw.scl, region.height * bits[2] * Draw.scl, req.rotation * 90);
        Draw.rect(topRegion, req.drawx(), req.drawy(), region.width * bits[1] * Draw.scl, region.height * bits[2] * Draw.scl, req.rotation * 90);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{regions[0][0], topRegions[0]};
    }

    public class CoveredConveyorBuild extends ConveyorBuild{

        @Override
        public void draw(){
            super.draw();
            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegions[blendbits], x, y, tilesize * blendsclx, tilesize * blendscly, rotation * 90);
        }

        @Override
        public void unitOn(Unit unit){}
    }
}
