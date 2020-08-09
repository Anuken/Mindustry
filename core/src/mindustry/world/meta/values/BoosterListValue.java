package mindustry.world.meta.values;

import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.Cicon;
import mindustry.world.meta.*;

import static mindustry.Vars.content;

public class BoosterListValue implements StatValue{
    protected float reload, maxUsed, multiplier;
    protected boolean baseReload;
    protected Boolf<Liquid> filter;

    public BoosterListValue(float reload, float maxUsed, float multiplier, boolean baseReload, Boolf<Liquid> filter){
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
                if(!filter.get(liquid)) continue;

                c.image(liquid.icon(Cicon.medium)).size(3 * 8).padRight(4).right().top();
                c.add(liquid.localizedName).padRight(10).left().top();
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
