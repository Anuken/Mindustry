package io.anuke.mindustry.world.consumers;

import io.anuke.arc.collection.*;
import io.anuke.arc.func.Boolf;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.ui.MultiReqImage;
import io.anuke.mindustry.ui.ReqImage;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.values.LiquidFilterValue;

import static io.anuke.mindustry.Vars.content;

public class ConsumeLiquidFilter extends ConsumeLiquidBase{
    public final Boolf<Liquid> filter;

    public ConsumeLiquidFilter(Boolf<Liquid> liquid, float amount){
        super(amount);
        this.filter = liquid;
    }

    @Override
    public void applyLiquidFilter(Bits arr){
        content.liquids().each(filter, item -> arr.set(item.id));
    }

    @Override
    public void build(Tile tile, Table table){
        Array<Liquid> list = content.liquids().select(l -> !l.isHidden() && filter.get(l));
        MultiReqImage image = new MultiReqImage();
        list.each(liquid -> image.add(new ReqImage(liquid.icon(Cicon.medium), () -> tile.entity != null && tile.entity.liquids != null && tile.entity.liquids.get(liquid) >= use(tile.entity))));

        table.add(image).size(8 * 4);
    }

    @Override
    public String getIcon(){
        return "icon-liquid-consume";
    }

    @Override
    public void update(TileEntity entity){
        entity.liquids.remove(entity.liquids.current(), use(entity));
    }

    @Override
    public boolean valid(TileEntity entity){
        return entity != null && entity.liquids != null && filter.get(entity.liquids.current()) && entity.liquids.currentAmount() >= use(entity);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(booster ? BlockStat.booster : BlockStat.input, new LiquidFilterValue(filter, amount * timePeriod, timePeriod == 60f));
    }
}
