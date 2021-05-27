package mindustry.world.meta.values;

import arc.Core;
import arc.scene.ui.layout.*;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.ui.Cicon;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

public class FuelListValue implements StatValue{
    private final FuelCrafter smelter;

    public FuelListValue(FuelCrafter smelter){
        this.smelter = smelter;
    }

    @Override
    public void display(Table table){
        table.row();

        table.image(smelter.fuelItem.icon(Cicon.medium)).size(3 * 8).padRight(4).right().top();
        table.add(smelter.fuelItem.localizedName).padRight(10).left().top();

        table.table(t -> {
            t.left().defaults().padRight(3).left();

            t.add(Core.bundle.format("fuel.input", smelter.fuelPerItem));

            sep(t, Core.bundle.format("fuel.use", smelter.fuelPerCraft));

            sep(t, Core.bundle.format("fuel.capacity", smelter.fuelCapacity));

            if(smelter.attribute != null){
                sep(t, Core.bundle.get("fuel.affinity"));
                t.row();
                t.table(at -> {
                    Attribute attr = smelter.attribute;

                    at.left().defaults().padRight(3).left();
                    for(var block : Vars.content.blocks()
                        .select(block -> block instanceof Floor f && f.attributes.get(attr) != 0 && !(f.isLiquid && !smelter.floating))
                        .<Floor>as().with(s -> s.sort(f -> f.attributes.get(attr)))){
                        new FloorEfficiencyValue(block, block.attributes.get(attr) * smelter.fuelUseReduction / -100f, false, true).display(at);
                    }
                }).padTop(0).left().get().background(null);
            }
        }).padTop(-9).left().get().background(Tex.underline);
    }

    void sep(Table table, String text){
        table.row();
        table.add(text);
    }
}
