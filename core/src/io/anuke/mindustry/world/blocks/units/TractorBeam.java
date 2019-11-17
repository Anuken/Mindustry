package io.anuke.mindustry.world.blocks.units;

import io.anuke.arc.Core;
import io.anuke.arc.collection.EnumSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.graphics.Drawf;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import static io.anuke.mindustry.Vars.tilesize;

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
    public void setStats() {
        super.setStats();

        stats.add(BlockStat.range, repairRadius / tilesize, StatUnit.blocks);

        stats.add(BlockStat.boostEffect, repairRadius * 2 / tilesize, StatUnit.blocks);
    }

    @Override
    public void drawSelect(Tile tile){
        Log.info(tile.entity.cons.optionalValid());
        Drawf.dashCircle(tile.drawx(), tile.drawy(), tile.entity.cons.optionalValid() ? repairRadius * 2 : repairRadius, Pal.accent);
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
            entity.target = Units.closest(tile.getTeam(), tile.drawx(), tile.drawy(), entity.cons.optionalValid() ? repairRadius * 2 : repairRadius, unit -> true);
        }
    }
}
