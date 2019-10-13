package io.anuke.mindustry.world.consumers;

import io.anuke.arc.collection.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

public class ConsumeLiquid extends ConsumeLiquidBase{
    public final Liquid liquid;

    public ConsumeLiquid(Liquid liquid, float amount){
        super(amount);
        this.liquid = liquid;
    }

    protected ConsumeLiquid(){
        this(null, 0f);
    }

    @Override
    public void applyLiquidFilter(Bits filter){
        filter.set(liquid.id);
    }

    @Override
    public void build(Tile tile, Table table){
        table.add(new ReqImage(liquid.icon(Cicon.medium), () -> valid(tile.entity))).size(8 * 4);
    }

    @Override
    public String getIcon(){
        return "icon-liquid-consume";
    }

    @Override
    public void update(TileEntity entity){
        entity.liquids.remove(liquid, Math.min(use(entity), entity.liquids.get(liquid)));
    }

    @Override
    public boolean valid(TileEntity entity){
        return entity != null && entity.liquids != null && entity.liquids.get(liquid) >= use(entity);
    }

    @Override
    public void display(BlockStats stats){
        stats.add(booster ? BlockStat.booster : BlockStat.input, liquid, amount * timePeriod, timePeriod == 60);
    }
}
