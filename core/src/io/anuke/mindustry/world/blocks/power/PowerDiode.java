package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.world;

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

        if (!entity.connected()) return;
        if (entity.input() > entity.output()) {
            entity.transfer(entity.backGraph().getBatteryStored() * (entity.input() - entity.output()) / 2);
        }
    }

    @Override
    public void setBars() {
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
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-arrow"), Core.atlas.find(name)};
    }

    @Override
    public TileEntity newEntity(){
        return new DiodeEntity();
    }

    public static class DiodeEntity extends TileEntity{

        public Tile backTile(){
            return getNearby(tile, (tile.rotation() + 2) % 4);
        }
        public Tile frontTile(){
            return getNearby(tile, tile.rotation());
        }
        //
        public PowerGraph backGraph(){
            if (!backTile().block().hasPower) return null;
            return backTile().entity.power.graph;
        }
        public PowerGraph frontGraph(){
            if (!frontTile().block().hasPower) return null;
            return frontTile().entity.power.graph;
        }
        public boolean connected(){
            return backGraph() != null && frontGraph() != null;
        }
        //
        public float input(){
            return backGraph() == null ? 0f : backGraph().getBatteryStored() / backGraph().getTotalBatteryCapacity();
        }
        public float output(){
            return frontGraph() == null ? 0f : frontGraph().getBatteryStored() / frontGraph().getTotalBatteryCapacity();
        }
        public void transfer(float amount){
            backGraph().useBatteries(amount);
            frontGraph().chargeBatteries(amount);
        }
        //
        public Tile getNearby(Tile tile, int rotation){
            int x = tile.x;
            int y = tile.y;

            if(rotation == 0) return world.ltile(x + 1, y);
            if(rotation == 1) return world.ltile(x, y + 1);
            if(rotation == 2) return world.ltile(x - 1, y);
            if(rotation == 3) return world.ltile(x, y - 1);
            return null;
        }

    }
}
