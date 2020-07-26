package mindustry.world.blocks.production;

import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.world.meta.*;

public class Fracker extends SolidPump{
    public float itemUseTime = 100f;

    public @Load("@-liquid") TextureRegion liquidRegion;
    public @Load("@-rotator") TextureRegion rotatorRegion;
    public @Load("@-top") TextureRegion topRegion;

    public Fracker(String name){
        super(name);
        hasItems = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.productionTime, itemUseTime / 60f, StatUnit.seconds);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, rotatorRegion, topRegion};
    }

    public class FrackerEntity extends SolidPumpEntity{
        public float accumulator;

        @Override
        public void drawCracks(){}

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            super.drawCracks();

            Draw.color(result.color);
            Draw.alpha(liquids.get(result) / liquidCapacity);
            Draw.rect(liquidRegion, x, y);
            Draw.color();

            Draw.rect(rotatorRegion, x, y, pumpTime);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void updateTile(){
            if(consValid()){
                if(accumulator >= itemUseTime){
                    consume();
                    accumulator -= itemUseTime;
                }

                super.updateTile();
                accumulator += delta() * efficiency();
            }else{
                dumpLiquid(result);
            }
        }

        @Override
        public float typeLiquid(){
            return liquids.get(result);
        }
    }
}
