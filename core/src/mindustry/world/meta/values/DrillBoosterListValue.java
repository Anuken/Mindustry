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

public class DrillBoosterListValue implements StatValue{
    protected float drillSpeed, maxUsed, multiplier;
    protected boolean baseDrillSpeed;
    protected Boolf<Liquid> filter;

    public DrillBoosterListValue(float drillSpeed, float maxUsed, float multiplier, boolean baseDrillSpeed, Boolf<Liquid> filter){
        this.drillSpeed = drillSpeed;
        this.maxUsed = maxUsed;
        this.baseDrillSpeed = baseDrillSpeed;
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

                    float drillSpeedRate = (baseDrillSpeed ? 1f : 0f) + maxUsed * multiplier * liquid.heatCapacity;
                    float standardDrillSpeed = baseDrillSpeed ? drillSpeed : drillSpeed / (maxUsed * multiplier * 0.4f);
                    float result = standardDrillSpeed / (drillSpeed / drillSpeedRate);
                    bt.add(Core.bundle.format("blocks.drillboostspeed", Strings.fixed(result, 1)));
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
