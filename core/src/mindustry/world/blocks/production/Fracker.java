package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.meta.*;

public class Fracker extends SolidPump{
    public float itemUseTime = 100f;

    public TextureRegion liquidRegion;
    public TextureRegion rotatorRegion;
    public TextureRegion topRegion;

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
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find(name + "-liquid");
        rotatorRegion = Core.atlas.find(name + "-rotator");
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-rotator"), Core.atlas.find(name + "-top")};
    }

    public class FrackerEntity extends SolidPumpEntity{
        public float accumulator;

        @Override
        public void drawCracks(){}

        @Override
        public boolean shouldConsume(){
            return liquids.get(result) < liquidCapacity - 0.01f;
        }

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
