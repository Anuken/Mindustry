package mindustry.world.draw;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.liquid.*;

public class DrawLiquidTile extends DrawBlock{
    public Liquid drawLiquid;
    public float padding;
    public float padLeft = -1, padRight = -1, padTop = -1, padBottom = -1;
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
        LiquidBlock.drawTiledFrames(build.block.size, build.x, build.y, padLeft, padRight, padTop, padBottom, drawn, build.liquids.get(drawn) / build.block.liquidCapacity * alpha);
    }

    @Override
    public void load(Block block){
        if(padLeft < 0) padLeft = padding;
        if(padRight < 0) padRight = padding;
        if(padTop < 0) padTop = padding;
        if(padBottom < 0) padBottom = padding;
    }
}
