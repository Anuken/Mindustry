package mindustry.world.consumers;

import arc.struct.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import mindustry.entities.type.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.Cicon;
import mindustry.world.*;
import mindustry.world.meta.*;

public class ConsumeLiquid extends ConsumeLiquidBase{
    public final @NonNull Liquid liquid;

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
