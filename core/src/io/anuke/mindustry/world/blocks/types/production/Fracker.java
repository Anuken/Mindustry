package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;

public class Fracker extends SolidPump {
    protected Liquid inputLiquid;
    protected float inputLiquidUse;

    public Fracker(String name) {
        super(name);
    }

    @Override
    public void draw(Tile tile) {
        FrackerEntity entity = tile.entity();

        Draw.rect(name, tile.drawx(), tile.drawy());

        Draw.color(tile.entity.liquid.liquid.color);
        Draw.alpha(tile.entity.liquid.amount/liquidCapacity);
        Draw.rect(name + "-liquid", tile.drawx(), tile.drawy());
        Draw.color();

        Draw.rect(name + "-rotator", tile.drawx(), tile.drawy(), entity.pumpTime);
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{Draw.region(name), Draw.region(name + "-rotator"), Draw.region(name + "-top")};
    }

    @Override
    public void update(Tile tile) {
        FrackerEntity entity = tile.entity();

        if(entity.input >= inputLiquidUse * Timers.delta()){
            super.update(tile);
            entity.input -= inputLiquidUse * Timers.delta();
        }else{
            tryDumpLiquid(tile);
        }
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        if(liquid != inputLiquid){
            super.handleLiquid(tile, source, liquid, amount);
        }else{
            FrackerEntity entity = tile.entity();
            entity.input += amount;
        }
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        FrackerEntity entity = tile.entity();

        return (liquid == inputLiquid && entity.input < inputLiquidUse) || super.acceptLiquid(tile, source, liquid, amount);
    }

    @Override
    public TileEntity getEntity() {
        return new FrackerEntity();
    }

    public static class FrackerEntity extends SolidPumpEntity{
        public float input;
    }
}
