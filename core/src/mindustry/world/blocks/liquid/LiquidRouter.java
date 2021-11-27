package mindustry.world.blocks.liquid;

import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class LiquidRouter extends LiquidBlock{
    /** kept only for mod compatibility reasons; all vanilla blocks have this as true */
    public boolean newDrawing = false;
    public float liquidPadding = 0f;

    public @Load(value = "conduit-liquid-#", length = Liquid.animationFrames) TextureRegion[] gasRegions;

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
                    if(liquids.current().gas){
                        drawTiledGas(gasRegions, size, x, y, liquidPadding, liquids.current().color, liquids.currentAmount() / liquidCapacity);
                    }else{
                        Draw.color(liquids.current().color, liquids.currentAmount() / liquidCapacity);
                        Fill.square(x, y, size * tilesize/2f - liquidPadding);
                        Draw.color();
                    }
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
