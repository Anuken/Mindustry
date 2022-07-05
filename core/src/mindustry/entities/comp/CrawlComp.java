package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class CrawlComp implements Posc, Rotc, Hitboxc, Unitc{
    @Import float x, y, speedMultiplier, rotation, hitSize;
    @Import UnitType type;
    @Import Team team;
    @Import Vec2 vel;

    transient Floor lastDeepFloor;
    transient float lastCrawlSlowdown = 1f;
    transient float segmentRot, crawlTime = Mathf.random(100f);

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
        return (on.isDeep() ? 0.45f : on.speedMultiplier) * speedMultiplier * lastCrawlSlowdown;
    }

    @Override
    public void add(){
        //reset segment rotation on add
        segmentRot = rotation;
    }

    @Override
    @Replace
    public Floor drownFloor(){
        return lastDeepFloor;
    }

    @Override
    public void update(){
        if(moving()){
            segmentRot = Angles.moveToward(segmentRot, rotation, type.segmentRotSpeed);

            int radius = (int)Math.max(0, hitSize / tilesize * 2f);
            int count = 0, solids = 0, deeps = 0;
            lastDeepFloor = null;

            //calculate tiles under this unit, and apply slowdown + particle effects
            for(int cx = -radius; cx <= radius; cx++){
                for(int cy = -radius; cy <= radius; cy++){
                    if(cx*cx + cy*cy <= radius){
                        count ++;
                        Tile t = Vars.world.tileWorld(x + cx*tilesize, y + cy*tilesize);
                        if(t != null){

                            if(t.solid()){
                                solids ++;
                            }

                            if(t.floor().isDeep()){
                                deeps ++;
                                lastDeepFloor = t.floor();
                            }

                            //TODO area damage to units
                            if(t.build != null && t.build.team != team){
                                t.build.damage(team, type.crushDamage * Time.delta);
                            }

                            if(Mathf.chanceDelta(0.025)){
                                Fx.crawlDust.at(t.worldx(), t.worldy(), t.floor().mapColor);
                            }
                        }else{
                            solids ++;
                        }
                    }
                }
            }

            //when most blocks under this unit cannot be drowned in, do not drown
            if((float)deeps / count < 0.75f){
                lastDeepFloor = null;
            }

            lastCrawlSlowdown = Mathf.lerp(1f, type.crawlSlowdown, Mathf.clamp((float)solids / count / type.crawlSlowdownFrac));
        }
        segmentRot = Angles.clampRange(segmentRot, rotation, type.segmentMaxRot);

        crawlTime += vel.len();
    }
}
