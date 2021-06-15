package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
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
    public void applyLiquidFilter(Bits arr){
        content.liquids().each(filter, item -> arr.set(item.id));
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
    public String getIcon(){
        return "icon-liquid-consume";
    }

    @Override
    public void update(Building entity){
        entity.liquids.remove(entity.liquids.current(), use(entity));
    }

    @Override
    public boolean valid(Building entity){
        return entity != null && entity.liquids != null && filter.get(entity.liquids.current()) && entity.liquids.currentAmount() >= use(entity);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquids(filter, amount * 60f, true));
    }
}
