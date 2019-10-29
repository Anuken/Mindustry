package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.Core;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.Attribute;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HeatedSmelter extends GenericSmelter{

    protected float pyratiteHeatBoost = 0.1f;
    protected float pyratiteHeatDecay = 0.001f;

    public HeatedSmelter(String name){
        super(name);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){

        if(item == Items.pyratite && ((HeatedSmelterEntity) tile.entity).boost() < 1f){
            return true;
        }

        return super.acceptItem(item, tile, source);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){

        if(item == Items.pyratite){
            ((HeatedSmelterEntity) tile.entity).pyratite += pyratiteHeatBoost;
            return;
        }

        super.handleItem(item, tile, source);
    }

    @Override
    public void update(Tile tile){
        HeatedSmelterEntity entity = (HeatedSmelterEntity) tile.entity;

        if(entity.pyratite > 0f){
            entity.pyratite = Mathf.clamp(entity.pyratite - pyratiteHeatDecay, 0f, 2f);
        }

        super.update(tile);
    }

    @Override
    protected float getProgressIncrease(TileEntity entity, float baseTime){
        return super.getProgressIncrease(entity, baseTime) + ((((HeatedSmelterEntity) entity).boost()) * 10);
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);

        HeatedSmelterEntity entity = tile.entity();
        entity.heat = sumAttribute(Attribute.heat, tile.x, tile.y);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPlaceText(Core.bundle.formatFloat("bar.heat.percentage", sumAttribute(Attribute.heat, x, y) * 10, 1), x, y, valid);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("heat", entity -> new Bar("bar.heat", Pal.lightOrange, ((HeatedSmelterEntity) entity)::boost));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.productionTime, 0f, StatUnit.heat);
        stats.add(BlockStat.productionTime, 0.5f, StatUnit.seconds);
        stats.add(BlockStat.productionTime, 100f, StatUnit.heat);
        stats.add(BlockStat.booster, Items.pyratite);
        stats.add(BlockStat.boostEffect, pyratiteHeatBoost * 100, StatUnit.heat);
    }

    @Override
    public TileEntity newEntity(){
        return new HeatedSmelterEntity();
    }

    public static class HeatedSmelterEntity extends GenericCrafterEntity{
        public float heat = 0.0f;
        public float pyratite = 0.0f;

        public float boost(){
            return Mathf.clamp(heat / 10 + pyratite);
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(heat);
            stream.writeFloat(pyratite);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            heat = stream.readFloat();
            pyratite = stream.readFloat();
        }
    }
}
