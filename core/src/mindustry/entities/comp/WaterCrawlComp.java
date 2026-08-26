package mindustry.entities.comp;

import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

@Component
abstract class WaterCrawlComp implements Posc, Velc, Hitboxc, Unitc, Crawlc{
    @Import float x, y, rotation, speedMultiplier;
    @Import UnitType type;

    @Replace
    public SolidPred solidity(){
        return isFlying() || ignoreSolids() ? null : EntityCollisions::waterSolid;
    }

    @Replace
    public boolean onSolid(){
        return EntityCollisions.waterSolid(tileX(), tileY());
    }

    @Replace
    public float floorSpeedMultiplier(){
        Floor on = isFlying() ? Blocks.air.asFloor() : floorOn();
        return (on.shallow ? 1f : 1.3f) * speedMultiplier;
    }

    public boolean onLiquid(){
        Tile tile = tileOn();
        return tile != null && tile.floor().isLiquid;
    }
}

