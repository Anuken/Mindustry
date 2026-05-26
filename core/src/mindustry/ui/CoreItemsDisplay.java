package mindustry.ui;

import arc.Core;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class CoreItemsDisplay extends Table{
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private CoreBuild core;

    public CoreItemsDisplay(){
        rebuild();
    }

    public void resetUsed(){
        usedItems.clear();
        background(null);
    }

    void rebuild(){
        clear();
        if(usedItems.size > 0){
            background(Styles.black6);
            margin(4);
        }

        update(() -> {
            core = Vars.player.team().core();

            if(content.items().contains(item -> core != null && core.items.get(item) > 0 && usedItems.add(item))){
                rebuild();
            }
        });

        int i = 0;
        int itemsPerRow = Core.graphics.getWidth() < 1600 ? 4 : 8;
        for(Item item : content.items()){
            if(usedItems.contains(item)){
                image(item.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));
                //TODO leaks garbage
                label(() -> core == null ? "0" : UI.formatAmount(core.items.get(item))).padRight(3).minWidth(52f).left().tooltip(t -> t.background(Styles.black6).margin(4f).label(() -> core == null ? "0" : core.items.get(item) + "").style(Styles.outlineLabel));
                //Change from 4 in each row to 8
                if(++i % itemsPerRow == 0){
                    row();
                }
            }
        }

    }
}
