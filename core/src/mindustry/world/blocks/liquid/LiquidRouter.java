package mindustry.world.blocks.liquid;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class LiquidRouter extends LiquidBlock{
    public float liquidPadding = 0f;

    public LiquidRouter(String name){
        super(name);
        solid = true;
        noUpdateDisabled = true;
        canOverdrive = false;
        floating = true;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{bottomRegion, region};
    }

    public class LiquidRouterBuild extends LiquidBuild{

        public final void updateLiquidRouter(){
            if(!enabled) return;

            dumpLiquid(liquids.current());
        }

        @Override
        public void addToList(){
            state.buildings.liquidRouters.add(this);
        }

        @Override
        public void removeFromList(){
            state.buildings.liquidRouters.remove(this);
        }

        @Override
        public void draw(){
            Draw.rect(bottomRegion, x, y);

            if(liquids.currentAmount() > 0.001f){
                drawTiledFrames(size, x, y, liquidPadding, liquids.current(), liquids.currentAmount() / liquidCapacity);
            }

            Draw.rect(region, x, y);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return (liquids.current() == liquid || liquids.currentAmount() < 0.2f);
        }
    }
}
