package mindustry.world.blocks.power;

import arc.Core;
import arc.math.Mathf;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.ui.Bar;
import arc.util.Eachable;
import mindustry.ui.Cicon;
import mindustry.world.Tile;
import mindustry.world.Block;
import arc.graphics.g2d.Draw;
import mindustry.graphics.Pal;
import arc.graphics.g2d.TextureRegion;
import mindustry.world.meta.*;

public class PowerDiode extends Block{
    public TextureRegion arrow;

    public PowerDiode(String name){
        super(name);
        rotate = true;
        update = true;
        solid = true;
        insulated = true;
        group = BlockGroup.power;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("back", entity -> new Bar("bar.input", Pal.powerBar, () -> bar(entity.back())));
        bars.add("front", entity -> new Bar("bar.output", Pal.powerBar, () -> bar(entity.front())));
    }

    @Override
    public void load(){
        super.load();
        arrow = Core.atlas.find(name + "-arrow");
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list) {
        TextureRegion reg = icon(Cicon.full);
        Draw.rect(icon(Cicon.full), req.drawx(), req.drawy(),
                reg.getWidth() * req.animScale * Draw.scl,
                reg.getHeight() * req.animScale * Draw.scl,
                0);
        Draw.rect(arrow, req.drawx(), req.drawy(),
                arrow.getWidth() * req.animScale * Draw.scl,
                arrow.getHeight() * req.animScale * Draw.scl,
                !rotate ? 0 : req.rotation * 90);
    }

    // battery % of the graph on either side, defaults to zero
    public float bar(Tilec tile){
        return (tile != null && tile.block().hasPower) ? tile.power().graph.getBatteryStored() / tile.power().graph.getTotalBatteryCapacity() : 0f;
    }

    public class PowerDiodeEntity extends TileEntity{
        @Override
        public void draw(){
            Draw.rect(region, x, y, 0);
            Draw.rect(arrow, x, y, rotate ? tile.rotation() * 90 : 0);
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(tile.front() == null || tile.back() == null || !tile.back().block().hasPower || !tile.front().block().hasPower || tile.back().team() != tile.front().team()) return;

            PowerGraph backGraph = tile.back().power().graph;
            PowerGraph frontGraph = tile.front().power().graph;
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
