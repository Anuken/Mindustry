package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.util.Eachable;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

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

        Tile back = back(tile);
        Tile front = front(tile);

        if(!back.block().hasPower || !front.block().hasPower) return;

        PowerGraph backGraph = back.entity.power.graph;
        PowerGraph frontGraph = front.entity.power.graph;
        if(backGraph == frontGraph) return;

        // 0f - 1f of battery capacity in use
        Float backStored = backGraph.getBatteryStored() / backGraph.getTotalBatteryCapacity();
        Float frontStored = frontGraph.getBatteryStored() / frontGraph.getTotalBatteryCapacity();

        // try to send if the input has more % capacity stored
        if(backStored > frontStored) {
            // send half of the difference
            float amount = backGraph.getBatteryStored() * (backStored - frontStored) / 2;

            backGraph.useBatteries(amount);
            frontGraph.chargeBatteries(amount);
        }
    }

    protected Tile back(Tile tile){
        return tile.getNearbyLink((tile.rotation() + 2) % 4);
    }

    protected Tile front(Tile tile){
        return tile.getNearbyLink(tile.rotation());
    }

    protected float bar(Tile tile){
        if(!tile.block().hasPower){
            return 0f;
        }else{
            return tile.entity.power.graph.getBatteryStored() / tile.entity.power.graph.getTotalBatteryCapacity();
        }
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("back", entity -> new Bar("bar.input", Pal.lighterOrange, () -> bar(back(entity.tile))) );
        bars.add("front", entity -> new Bar("bar.output", Pal.lighterOrange, () -> bar(front(entity.tile))) );
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
