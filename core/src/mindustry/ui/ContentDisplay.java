package mindustry.ui;

import arc.*;
import arc.struct.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class ContentDisplay{

    public static void displayBlock(Table table, Block block){

        table.table(title -> {
            int size = 8 * 6;

            title.image(block.icon(Cicon.xlarge)).size(size);
            title.add("[accent]" + block.localizedName).padLeft(5);
        });

        table.row();

        table.image().height(3).color(Color.lightGray).pad(8).padLeft(0).padRight(0).fillX();

        table.row();

        if(block.description != null){
            table.add(block.displayDescription()).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.image().height(3).color(Color.lightGray).pad(8).padLeft(0).padRight(0).fillX();
            table.row();
        }

        BlockStats stats = block.stats;

        for(StatCategory cat : stats.toMap().keys()){
            OrderedMap<BlockStat, Seq<StatValue>> map = stats.toMap().get(cat);

            if(map.size == 0) continue;

            table.add("$category." + cat.name()).color(Pal.accent).fillX();
            table.row();

            for(BlockStat stat : map.keys()){
                table.table(inset -> {
                    inset.left();
                    inset.add("[lightgray]" + stat.localized() + ":[] ").left();
                    Seq<StatValue> arr = map.get(stat);
                    for(StatValue value : arr){
                        value.display(inset);
                        inset.add().size(10f);
                    }

                }).fillX().padLeft(10);
                table.row();
            }
        }
    }

    public static void displayItem(Table table, Item item){

        table.table(title -> {
            title.image(item.icon(Cicon.xlarge)).size(8 * 6);
            title.add("[accent]" + item.localizedName).padLeft(5);
        });

        table.row();

        table.image().height(3).color(Color.lightGray).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(item.description != null){
            table.add(item.displayDescription()).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.image().height(3).color(Color.lightGray).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        table.left().defaults().fillX();

        table.add(Core.bundle.format("item.explosiveness", (int)(item.explosiveness * 100)));
        table.row();
        table.add(Core.bundle.format("item.flammability", (int)(item.flammability * 100)));
        table.row();
        table.add(Core.bundle.format("item.radioactivity", (int)(item.radioactivity * 100)));
        table.row();
    }

    public static void displayLiquid(Table table, Liquid liquid){

        table.table(title -> {
            title.image(liquid.icon(Cicon.xlarge)).size(8 * 6);
            title.add("[accent]" + liquid.localizedName).padLeft(5);
        });

        table.row();

        table.image().height(3).color(Color.lightGray).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(liquid.description != null){
            table.add(liquid.displayDescription()).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.image().height(3).color(Color.lightGray).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        table.left().defaults().fillX();

        table.add(Core.bundle.format("item.explosiveness", (int)(liquid.explosiveness * 100)));
        table.row();
        table.add(Core.bundle.format("item.flammability", (int)(liquid.flammability * 100)));
        table.row();
        table.add(Core.bundle.format("liquid.heatcapacity", (int)(liquid.heatCapacity * 100)));
        table.row();
        table.add(Core.bundle.format("liquid.temperature", (int)(liquid.temperature * 100)));
        table.row();
        table.add(Core.bundle.format("liquid.viscosity", (int)(liquid.viscosity * 100)));
        table.row();
    }

    public static void displayUnit(Table table, UnitType unit){
        table.table(title -> {
            title.image(unit.icon(Cicon.xlarge)).size(8 * 6);
            title.add("[accent]" + unit.localizedName).padLeft(5);
        });

        table.row();

        table.image().height(3).color(Color.lightGray).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(unit.description != null){
            table.add(unit.displayDescription()).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.image().height(3).color(Color.lightGray).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        table.left().defaults().fillX();

        table.add(Core.bundle.format("unit.health", unit.health));
        table.row();
        table.add(Core.bundle.format("unit.speed", Strings.fixed(unit.speed, 1)));
        table.row();
        table.row();
    }
}
