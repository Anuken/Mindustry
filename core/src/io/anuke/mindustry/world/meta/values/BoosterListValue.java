package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.*;
import io.anuke.arc.function.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.content;

public class BoosterListValue implements StatValue{
    protected float reload, maxUsed, multiplier;
    protected boolean baseReload;
    protected Predicate<Liquid> filter;

    public BoosterListValue(float reload, float maxUsed, float multiplier, boolean baseReload, Predicate<Liquid> filter){
        this.reload = reload;
        this.maxUsed = maxUsed;
        this.baseReload = baseReload;
        this.multiplier = multiplier;
        this.filter = filter;
    }

    @Override
    public void display(Table table){

        table.row();
        table.table(c -> {
            for(Liquid liquid : content.liquids()){
                if(!filter.test(liquid)) continue;

                c.addImage(liquid.icon(Cicon.medium)).size(3 * 8).padRight(4).right().top();
                c.add(liquid.localizedName()).padRight(10).left().top();
                c.table(Tex.underline, bt -> {
                    bt.left().defaults().padRight(3).left();

                    float reloadRate = (baseReload ? 1f : 0f) + maxUsed * multiplier * liquid.heatCapacity;
                    float standardReload = baseReload ? reload : reload / (maxUsed * multiplier * 0.4f);
                    float result = standardReload / (reload / reloadRate);
                    bt.add(Core.bundle.format("bullet.reload", Strings.fixed(result, 1)));
                }).left().padTop(-9);
                c.row();
            }
        }).colspan(table.getColumns());
        table.row();

    }

    void sep(Table table, String text){
        table.row();
        table.add(text);
    }
}
