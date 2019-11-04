package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.util.Eachable;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.entities.type.TileEntity;
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

        DiodeEntity entity = (DiodeEntity) tile.entity;

        if(!entity.connected()) return;
        if(entity.input() > entity.output()){
            entity.transfer(entity.graph(entity.back()).getBatteryStored() * (entity.input() - entity.output()) / 2);
        }
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("back", entity -> new Bar("bar.input", Pal.lighterOrange, () -> ((DiodeEntity) entity).input()) );
        bars.add("front", entity -> new Bar("bar.output", Pal.lighterOrange, () -> ((DiodeEntity) entity).output()) );
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

    @Override
    public TileEntity newEntity(){
        return new DiodeEntity();
    }

    public static class DiodeEntity extends TileEntity{

        public Tile back(){
            return tile.getNearbyLink(tile, (tile.rotation() + 2) % 4);
        }

        public Tile front(){
            return tile.getNearbyLink(tile, tile.rotation());
        }

        private PowerGraph graph(Tile tile){
            if(!tile.block().hasPower) return null;
            return tile.entity.power.graph;
        }

        public boolean connected(){
            return graph(back()) != null && graph(front()) != null;
        }

        public float input(){
            return battery(graph(back()));
        }

        public float output(){
            return battery(graph(front()));
        }

        public float battery(PowerGraph graph){
            return graph == null ? 0f : graph.getBatteryStored() / graph.getTotalBatteryCapacity();
        }

        public void transfer(float amount){
            graph(back()).useBatteries(amount);
            graph(front()).chargeBatteries(amount);
        }
    }
}
