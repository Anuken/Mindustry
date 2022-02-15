package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ConsumeLiquidFilter extends ConsumeLiquidBase{
    public Boolf<Liquid> filter;

    public ConsumeLiquidFilter(Boolf<Liquid> liquid, float amount){
        super(amount);
        this.filter = liquid;
    }

    public ConsumeLiquidFilter(){
        this.filter = l -> false;
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
        list.each(liquid -> image.add(new ReqImage(liquid.uiIcon, () ->
            build.liquids != null && build.liquids.current() == liquid && build.liquids.get(liquid) >= Math.max(use(build), amount * build.delta()))));

        table.add(image).size(8 * 4);
    }

    @Override
    public void update(Building build){
        build.liquids.remove(build.liquids.current(), use(build));
    }

    @Override
    public boolean valid(Building build){
        return build != null && build.liquids != null && filter.get(build.liquids.current()) && build.liquids.currentAmount() >= use(build);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquids(filter, amount * 60f, true));
    }
}
