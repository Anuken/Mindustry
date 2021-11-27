package mindustry.world.blocks.liquid;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.type.*;

public class LiquidRouter extends LiquidBlock{
    /** kept only for mod compatibility reasons; all vanilla blocks have this as true */
    public boolean newDrawing = false;
    public float liquidPadding = 0f;

    public LiquidRouter(String name){
        super(name);

        noUpdateDisabled = true;
        canOverdrive = false;
    }

    @Override
    public TextureRegion[] icons(){
        return newDrawing ? new TextureRegion[]{bottomRegion, region} : super.icons();
    }

    public class LiquidRouterBuild extends LiquidBuild{
        @Override
        public void updateTile(){
            if(liquids.currentAmount() > 0.01f){
                dumpLiquid(liquids.current());
            }
        }

        @Override
        public void draw(){
            if(newDrawing){
                Draw.rect(bottomRegion, x, y);

                if(liquids.currentAmount() > 0.001f){
                    drawTiledFrames(size, x, y, liquidPadding, liquids.current(), liquids.currentAmount() / liquidCapacity);
                }

                Draw.rect(region, x, y);
            }else{
                super.draw();
            }

        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return (liquids.current() == liquid || liquids.currentAmount() < 0.2f);
        }
    }
}
