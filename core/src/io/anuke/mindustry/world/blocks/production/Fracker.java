package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeItem;
import io.anuke.arc.graphics.g2d.Draw;

public class Fracker extends SolidPump{
    protected final float itemUseTime = 100f;

    protected TextureRegion liquidRegion;
    protected TextureRegion rotatorRegion;
    protected TextureRegion topRegion;

    public Fracker(String name){
        super(name);
        hasItems = true;
        singleLiquid = false;

        consumes.require(ConsumeItem.class);
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find(name + "-liquid");
        rotatorRegion = Core.atlas.find(name + "-rotator");
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void draw(Tile tile){
        FrackerEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy());

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
        Item item = consumes.item();

        while(entity.accumulator >= itemUseTime && entity.items.has(item, 1)){
            entity.items.remove(item, 1);
            entity.accumulator -= itemUseTime;
        }

        if(entity.cons.valid() && entity.accumulator < itemUseTime){
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
