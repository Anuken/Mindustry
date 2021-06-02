package mindustry.world.consumers;

import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

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
    public void build(Building tile, Table table){
        table.add(new ReqImage(liquid.uiIcon, () -> valid(tile))).size(iconMed).top().left();
    }

    @Override
    public String getIcon(){
        return "icon-liquid-consume";
    }

    @Override
    public void update(Building entity){
        entity.liquids.remove(liquid, Math.min(use(entity), entity.liquids.get(liquid)));
    }

    @Override
    public boolean valid(Building entity){
        return entity != null && entity.liquids != null && entity.liquids.get(liquid) >= amount * entity.delta();
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, liquid, amount * 60f, true);
    }
}
