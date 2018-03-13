package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.graphics.Draw;

public class Conduit extends LiquidBlock {
    protected final int timerFlow = timers++;

    public Conduit(String name) {
        super(name);
        liquidRegion = "conduit-liquid";
    }

    @Override
    public void update(Tile tile){
        if(tile.entity.liquid.amount > 0.001f && tile.entity.timer.get(timerFlow, 1)){
            tryMoveLiquid(tile, tile.getNearby(tile.getRotation()));
        }
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name() + "-bottom"), Draw.region(name() + "-top")};
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return super.acceptLiquid(tile, source, liquid, amount) && ((2 + source.relativeTo(tile.x, tile.y)) % 4 != tile.getRotation());
    }
}
