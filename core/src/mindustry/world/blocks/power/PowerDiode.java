package mindustry.world.blocks.power;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class PowerDiode extends Block{
    public @Load("@-arrow") TextureRegion arrow;

    public PowerDiode(String name){
        super(name);
        rotate = true;
        update = true;
        solid = true;
        insulated = true;
        group = BlockGroup.power;
        noUpdateDisabled = true;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("back", entity -> new Bar("bar.input", Pal.powerBar, () -> bar(entity.back())));
        bars.add("front", entity -> new Bar("bar.output", Pal.powerBar, () -> bar(entity.front())));
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(icon(Cicon.full), req.drawx(), req.drawy());
        Draw.rect(arrow, req.drawx(), req.drawy(), !rotate ? 0 : req.rotation * 90);
    }

    // battery % of the graph on either side, defaults to zero
    public float bar(Building tile){
        return (tile != null && tile.block.hasPower) ? tile.power.graph.getLastPowerStored() / tile.power.graph.getTotalBatteryCapacity() : 0f;
    }

    public class PowerDiodeBuild extends Building{
        @Override
        public void draw(){
            Draw.rect(region, x, y, 0);
            Draw.rect(arrow, x, y, rotate ? rotdeg() : 0);
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(front() == null || back() == null || !back().block.hasPower || !front().block.hasPower || back().team != front().team) return;

            PowerGraph backGraph = back().power.graph;
            PowerGraph frontGraph = front().power.graph;
            if(backGraph == frontGraph) return;

            // 0f - 1f of battery capacity in use
            float backStored = backGraph.getBatteryStored() / backGraph.getTotalBatteryCapacity();
            float frontStored = frontGraph.getBatteryStored() / frontGraph.getTotalBatteryCapacity();

            // try to send if the back side has more % capacity stored than the front side
            if(backStored > frontStored) {
                // send half of the difference
                float amount = backGraph.getBatteryStored() * (backStored - frontStored) / 2;
                // prevent sending more than the front can handle
                amount = Mathf.clamp(amount, 0, frontGraph.getTotalBatteryCapacity() * (1 - frontStored));

                backGraph.useBatteries(amount);
                frontGraph.chargeBatteries(amount);
            }
        }
    }
}
