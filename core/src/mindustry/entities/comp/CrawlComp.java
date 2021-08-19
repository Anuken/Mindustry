package mindustry.entities.comp;

import arc.math.geom.*;
import mindustry.ai.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;

//TODO
@Component
abstract class CrawlComp implements Posc, Rotc, Hitboxc, Unitc{
    @Import float x, y, speedMultiplier;
    @Import UnitType type;
    @Import Vec2 vel;

    //TODO segments
    float segmentRot;
    float crawlTime;

    @Replace
    @Override
    public SolidPred solidity(){
        return EntityCollisions::legsSolid;
    }

    @Override
    @Replace
    public int pathType(){
        return Pathfinder.costLegs;
    }

    @Override
    @Replace
    public float floorSpeedMultiplier(){
        Floor on = isFlying() ? Blocks.air.asFloor() : floorOn();
        //TODO take into account extra blocks
        return on.speedMultiplier * speedMultiplier;
    }

    @Override
    public void update(){
        crawlTime += vel.len();
    }
}
