package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

public class Fracker extends SolidPump{
    protected final float itemUseTime = 100f;

    protected TextureRegion liquidRegion;
    protected TextureRegion rotatorRegion;
    protected TextureRegion topRegion;

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
    public void drawCracks(Tile tile){}

    @Override
    public boolean canProduce(Tile tile){
        return tile.entity.liquids.get(result) < liquidCapacity - 0.01f;
    }

    @Override
    public void draw(Tile tile){
        FrackerEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy());
        super.drawCracks(tile);

        Draw.color(result.color);
        Draw.alpha(tile.entity.liquids.get(result) / liquidCapacity);
        Draw.rect(liquidRegion, tile.drawx(), tile.drawy());
        Draw.color();

        Draw.rect(rotatorRegion, tile.drawx(), tile.drawy(), entity.pumpTime);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-rotator"), Core.atlas.find(name + "-top")};
    }

    @Override
    public void update(Tile tile){
        FrackerEntity entity = tile.entity();

        if(entity.cons.valid()){
            if(entity.accumulator >= itemUseTime){
                entity.cons.trigger();
                entity.accumulator -= itemUseTime;
            }

            super.update(tile);
            entity.accumulator += entity.delta() * entity.power.satisfaction;
        }else{
            tryDumpLiquid(tile, result);
        }
    }

    @Override
    public TileEntity newEntity(){
        return new FrackerEntity();
    }

    @Override
    public float typeLiquid(Tile tile){
        return tile.entity.liquids.get(result);
    }

    public static class FrackerEntity extends SolidPumpEntity{
        public float accumulator;
    }
}
