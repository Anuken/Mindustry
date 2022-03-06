package mindustry.world.draw;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.liquid.*;

/** Not standalone. */
public class DrawLiquidTile extends DrawBlock{
    public Liquid drawLiquid;
    public float padding;
    public float alpha = 1f;

    public DrawLiquidTile(Liquid drawLiquid, float padding){
        this.drawLiquid = drawLiquid;
        this.padding = padding;
    }

    public DrawLiquidTile(Liquid drawLiquid){
        this.drawLiquid = drawLiquid;
    }

    public DrawLiquidTile(){
    }

    @Override
    public void draw(Building build){
        Liquid drawn = drawLiquid != null ? drawLiquid : build.liquids.current();
        LiquidBlock.drawTiledFrames(build.block.size, build.x, build.y, padding, drawn, build.liquids.get(drawn) / build.block.liquidCapacity * alpha);
    }
}
