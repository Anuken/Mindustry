package mindustry.world.meta.values;

import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BoosterListValue implements StatValue{
    protected float efficiency, maxUsed, multiplier;
    protected boolean baseEfficiency;
    protected Boolf<Liquid> filter;

    public BoosterListValue(float efficiency, float maxUsed, float multiplier, boolean baseEfficiency, Boolf<Liquid> filter){
        this.efficiency = efficiency;
        this.maxUsed = maxUsed;
        this.baseEfficiency = baseEfficiency;
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

                    float reloadRate = (baseEfficiency ? 1f : 0f) + maxUsed * multiplier * liquid.heatCapacity;
                    float standardReload = baseEfficiency ? efficiency : efficiency / (maxUsed * multiplier * 0.4f);
                    float result = standardReload / (efficiency / reloadRate);
                    bt.add(Core.bundle.format("blocks.efficiency", Strings.fixed(result, 1)));
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
