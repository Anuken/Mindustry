package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.ui.Bar;
import io.anuke.arc.util.Eachable;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.Block;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.traits.BuilderTrait;

public class PowerDiode extends Block{
    protected TextureRegion arrow;

    public PowerDiode(String name){
        super(name);
        rotate = true;
        update = true;
        solid = true;
        insulated = true;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        if(!tile.back().block().hasPower || !tile.front().block().hasPower) return;

        PowerGraph backGraph = tile.back().entity.power.graph;
        PowerGraph frontGraph = tile.front().entity.power.graph;
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

    // battery % of the graph on either side, defaults to zero
    protected float bar(Tile tile){
        return tile.block().hasPower ? tile.entity.power.graph.getBatteryStored() / tile.entity.power.graph.getTotalBatteryCapacity() : 0f;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("back", entity -> new Bar("bar.input", Pal.lighterOrange, () -> bar(entity.tile.back())) );
        bars.add("front", entity -> new Bar("bar.output", Pal.lighterOrange, () -> bar(entity.tile.front())) );
    }

    @Override
    public void load(){
        super.load();
        arrow = Core.atlas.find(name + "-arrow");
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(region, tile.drawx(), tile.drawy(), 0);
        Draw.rect(arrow, tile.drawx(), tile.drawy(), rotate ? tile.rotation() * 90 : 0);
    }

    @Override
    public void drawRequestRegion(BuilderTrait.BuildRequest req, Eachable<BuilderTrait.BuildRequest> list) {
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
}
