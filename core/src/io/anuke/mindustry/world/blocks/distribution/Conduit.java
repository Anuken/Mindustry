package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.mindustry.world.modules.LiquidModule;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Conduit extends LiquidBlock {
    protected final int timerFlow = timers++;

    public Conduit(String name) {
        super(name);
        rotate = true;
        solid = false;
    }

    @Override
    public void load() {
        super.load();

        liquidRegion = Draw.region("conduit-liquid");
    }

    @Override
    public void draw(Tile tile){
        ConduitEntity entity = tile.entity();
        LiquidModule mod = tile.entity.liquids;

        int rotation = rotate ? tile.getRotation() * 90 : 0;

        Draw.rect(bottomRegion, tile.drawx(), tile.drawy(), rotation);

        Draw.color(mod.current().color);
        Draw.alpha(entity.smoothLiquid);
        Draw.rect(liquidRegion, tile.drawx(), tile.drawy(), rotation);
        Draw.color();

        Draw.rect(topRegion, tile.drawx(), tile.drawy(), rotation);
    }

    @Override
    public void update(Tile tile){
        ConduitEntity entity = tile.entity();
        entity.smoothLiquid = Mathf.lerpDelta(entity.smoothLiquid, entity.liquids.total()/liquidCapacity, 0.05f);

        if(tile.entity.liquids.total() > 0.001f && tile.entity.timer.get(timerFlow, 1)){
            tryMoveLiquid(tile, tile.getNearby(tile.getRotation()), true, tile.entity.liquids.current());
            entity.wakeUp();
        }else{
            entity.sleep();
        }
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name() + "-bottom"), Draw.region(name() + "-top")};
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        tile.entity.wakeUp();
        return super.acceptLiquid(tile, source, liquid, amount) && (tile.entity.liquids.current() == liquid || tile.entity.liquids.get(tile.entity.liquids.current()) < 0.01f) &&
                ((2 + source.relativeTo(tile.x, tile.y)) % 4 != tile.getRotation());
    }

    @Override
    public TileEntity getEntity() {
        return new ConduitEntity();
    }

    public static class ConduitEntity extends TileEntity {
        public float smoothLiquid;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeFloat(smoothLiquid);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            smoothLiquid = stream.readFloat();
        }
    }
}
