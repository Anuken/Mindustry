package io.anuke.mindustry.world.consumers;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.type.LiquidStack;
import io.anuke.mindustry.ui.ReqImage;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;

public class ConsumeLiquid extends Consume{
    protected final float use;
    protected final Liquid liquid;

    public ConsumeLiquid(Liquid liquid, float use){
        this.liquid = liquid;
        this.use = use;
    }

    @Override
    public void applyLiquidFilter(boolean[] filter){
        filter[liquid.id] = true;
    }

    public float used(){
        return use;
    }

    public Liquid get(){
        return liquid;
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.liquid;
    }

    @Override
    public void build(Tile tile, Table table){
        table.add(new ReqImage(liquid.getContentIcon(), () -> valid(tile.block(), tile.entity))).size(8*4);
    }

    @Override
    public String getIcon(){
        return "icon-liquid-small";
    }

    @Override
    public void update(Block block, TileEntity entity){
        entity.liquids.remove(liquid, Math.min(use(block, entity), entity.liquids.get(liquid)));
    }

    @Override
    public boolean valid(Block block, TileEntity entity){
        return entity != null && entity.liquids != null && entity.liquids.get(liquid) >= use(block, entity);
    }

    @Override
    public void display(BlockStats stats){
        //stats.add(BlockStat.liquidUse, use * 60f, StatUnit.liquidSecond);
        stats.add(boost ? BlockStat.booster : BlockStat.input, new LiquidStack(liquid, use * 60f));
    }

    float use(Block block, TileEntity entity){
        return Math.min(use * entity.delta(), block.liquidCapacity);
    }
}
