package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.defense.turrets.Turret;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatCategory;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class ContentDisplay {

    public static void displayRecipe(Table table, Recipe recipe){
        Block block = recipe.result;

        table.table(title -> {
            int size = 8*6;

            if(block instanceof Turret){
                size = (8 * block.size + 2) * (7 - block.size*2);
            }

            title.addImage(Draw.region("block-icon-" + block.name)).size(size);
            title.add("[accent]" + block.formalName).padLeft(5);
        });

        table.row();

        table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(block.fullDescription != null){
            table.add(block.fullDescription).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        BlockStats stats = block.stats;

        for(StatCategory cat : stats.toMap().keys()){
            OrderedMap<BlockStat, StatValue> map = stats.toMap().get(cat);

            if(map.size == 0) continue;

            table.add("$text.category." + cat.name()).color(Palette.accent).fillX();
            table.row();

            for (BlockStat stat : map.keys()){
                table.table(inset -> {
                    inset.left();
                    inset.add("[LIGHT_GRAY]" + stat.localized() + ":[] ");
                    map.get(stat).display(inset);
                }).fillX().padLeft(10);
                table.row();
            }
        }
    }

    public static void displayItem(Table table, Item item){

        table.table(title -> {
            title.addImage(item.getContentIcon()).size(8 * 6);
            title.add("[accent]" + item.localizedName()).padLeft(5);
        });

        table.row();

        table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(item.description != null){
            table.add(item.description).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        table.left().defaults().fillX();

        table.add(Bundles.format("text.item.explosiveness", (int)(item.explosiveness * 100)));
        table.row();
        table.add(Bundles.format("text.item.flammability", (int)(item.flammability * 100)));
        table.row();
        table.add(Bundles.format("text.item.radioactivity", (int)(item.radioactivity * 100)));
        table.row();
        table.add(Bundles.format("text.item.fluxiness", (int)(item.fluxiness * 100)));
        table.row();
        table.add(Bundles.format("text.item.hardness", item.hardness));
        table.row();
    }

    public static void displayLiquid(Table table, Liquid liquid){

    }

    public static void displayMech(Table table, Mech mech){

    }

    public static void displayUnit(Table table, UnitType unit){

    }
}
