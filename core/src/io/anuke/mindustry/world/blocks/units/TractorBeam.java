package io.anuke.mindustry.world.blocks.units;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.world.Tile;

public class TractorBeam extends RepairPoint {

    public TractorBeam(String name) {
        super(name);
        laserColor = Color.valueOf("00a2ff");
    }

    @Override
    public void update(Tile tile){
        RepairPointEntity entity = tile.entity();

        boolean targetIsBeingRepaired = false;
        if(entity.target != null && (entity.target.isDead() || entity.target.dst(tile) > repairRadius)){
            entity.target = null;
        }else if(entity.target != null && entity.cons.valid()){
            entity.target.health += repairSpeed * Time.delta() * entity.strength * entity.efficiency();
            entity.target.clampHealth();
            entity.rotation = Mathf.slerpDelta(entity.rotation, entity.angleTo(entity.target), 0.5f);
            targetIsBeingRepaired = true;
        }

        if(entity.target != null && targetIsBeingRepaired){
            entity.strength = Mathf.lerpDelta(entity.strength, 1f, 0.08f * Time.delta());
        }else{
            entity.strength = Mathf.lerpDelta(entity.strength, 0f, 0.07f * Time.delta());
        }

        if(entity.timer.get(timerTarget, 20)){
            rect.setSize(repairRadius * 2).setCenter(tile.drawx(), tile.drawy());
            entity.target = Units.closest(tile.getTeam(), tile.drawx(), tile.drawy(), repairRadius,
                    unit -> true);
        }
    }
}
