package mindustry.ui;

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
    }

    void rebuild(){
        clear();
        background(Styles.black6);
        margin(4);

        update(() -> {
            core = Vars.player.team().core();

            if(content.items().contains(item -> core != null && core.items.get(item) > 0 && usedItems.add(item))){
                rebuild();
            }
        });

        int i = 0;

        for(Item item : content.items()){
            if(usedItems.contains(item)){
                image(item.icon(Cicon.small)).padRight(3);
                //TODO leaks garbage
                label(() -> core == null ? "0" : UI.formatAmount(core.items.get(item))).padRight(3).left();

                if(++i % 4 == 0){
                    row();
                }
            }
        }

    }
}
