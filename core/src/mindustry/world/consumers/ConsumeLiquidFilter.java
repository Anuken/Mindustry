package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ConsumeLiquidFilter extends ConsumeLiquidBase{
    public Boolf<Liquid> filter = l -> false;

    public ConsumeLiquidFilter(Boolf<Liquid> liquid, float amount){
        super(amount);
        this.filter = liquid;
    }

    public ConsumeLiquidFilter(){
    }

    @Override
    public void apply(Block block){
        block.hasLiquids = true;
        content.liquids().each(filter, item -> block.liquidFilter[item.id] = true);
    }

    @Override
    public void build(Building build, Table table){
        Seq<Liquid> list = content.liquids().select(l -> !l.isHidden() && filter.get(l));
        MultiReqImage image = new MultiReqImage();
        list.each(liquid -> image.add(new ReqImage(liquid.uiIcon, () -> getConsumed(build) == liquid)));

        table.add(image).size(8 * 4);
    }

    @Override
    public void update(Building build){
        Liquid liq = getConsumed(build);
        if(liq != null){
            build.liquids.remove(liq, amount * build.edelta() * multiplier.get(build));
        }
    }

    @Override
    public float efficiency(Building build){
        var liq = getConsumed(build);
        float ed = build.edelta();
        if(ed <= 0.00000001f) return 0f;
        return liq != null ? Math.min(build.liquids.get(liq) / (amount * ed * multiplier.get(build)), 1f) : 0f;
    }
    
    public @Nullable Liquid getConsumed(Building build){
        if(filter.get(build.liquids.current()) && build.liquids.currentAmount() > 0){
            return build.liquids.current();
        }

        var liqs = content.liquids();

        for(int i = 0; i < liqs.size; i++){
            var liq = liqs.get(i);
            if(filter.get(liq) && build.liquids.get(liq) > 0){
                return liq;
            }
        }
        return null;
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquids(filter, amount * 60f, true));
    }
}
