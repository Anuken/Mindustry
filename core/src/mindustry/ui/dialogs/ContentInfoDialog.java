package mindustry.ui.dialogs;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class ContentInfoDialog extends BaseDialog{

    public ContentInfoDialog(){
        super("@info.title");

        addCloseButton();
    }

    public void show(UnlockableContent content){
        cont.clear();

        Table table = new Table();
        table.margin(10);

        //initialize stats if they haven't been yet
        content.checkStats();

        table.table(title1 -> {
            int size = 8 * 6;

            title1.image(content.icon(Cicon.xlarge)).size(size).scaling(Scaling.fit);
            title1.add("[accent]" + content.localizedName).padLeft(5);
        });

        table.row();

        if(content.description != null){
            table.add("@category.purpose").color(Pal.accent).fillX().padTop(10);
            table.row();
            table.add("[lightgray]" + content.displayDescription()).wrap().fillX().padLeft(10).width(500f).left();
            table.row();

            if(!content.stats.useCategories){
                table.add("@category.general").fillX().color(Pal.accent);
                table.row();
            }
        }

        Stats stats = content.stats;

        for(StatCat cat : stats.toMap().keys()){
            OrderedMap<Stat, Seq<StatValue>> map = stats.toMap().get(cat);

            if(map.size == 0) continue;

            //TODO check
            if(stats.useCategories){
                table.add("@category." + cat.name()).color(Pal.accent).fillX();
                table.row();
            }

            for(Stat stat : map.keys()){
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

        if(content.details != null){
            table.add("[gray]" + content.details).pad(6).padTop(20).width(400f).wrap().fillX();
            table.row();
        }

        ScrollPane pane = new ScrollPane(table);
        cont.add(pane);

        show();
    }

}
