package io.anuke.mindustry.world.blocks.units;

import io.anuke.arc.Core;
import io.anuke.arc.collection.EnumSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;

public class TractorBeam extends RepairPoint {

    public TractorBeam(String name) {
        super(name);
        flags = EnumSet.of(BlockFlag.tractor);
        laserColor = Color.white;
    }

    @Override
    public void load(){
        super.load();

        laser = Core.atlas.find("coldfusion-laser");
        laserEnd = Core.atlas.find("coldfusion-laser-end");
    }

    @Override
    public void update(Tile tile){
        RepairPointEntity entity = tile.entity();

        if(entity.target != null && (entity.target.isDead() || entity.target.dst(tile) > repairRadius)){
            entity.target = null;
        }else if(entity.target != null && entity.cons.valid()){
            entity.rotation = Mathf.slerpDelta(entity.rotation, entity.angleTo(entity.target), 0.5f);
            entity.strength = Mathf.lerpDelta(entity.strength, 1f, 0.08f * Time.delta());
        }

        if(entity.timer.get(timerTarget, 20)){
            rect.setSize(repairRadius * 2).setCenter(tile.drawx(), tile.drawy());
            entity.target = Units.closestEnemy(tile.getTeam(), tile.drawx(), tile.drawy(), repairRadius, Unit::isFlying);
        }
    }
}
