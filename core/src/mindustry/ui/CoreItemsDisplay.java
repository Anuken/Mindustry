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
            CoreEntity core = Vars.player.team().core();

            for(Item item : content.items()){
                if(core != null && core.items.get(item) > 0 && usedItems.add(item)){
                    rebuild();
                    break;
                }
            }
        });

        int i = 0;

        CoreEntity core = Vars.player.team().core();
        for(Item item : content.items()){
            if(usedItems.contains(item)){
                image(item.icon(Cicon.small)).padRight(3);
                label(() -> core == null ? "0" : UI.formatAmount(core.items.get(item))).padRight(3);

                if(++i % 4 == 0){
                    row();
                }
            }
        }

    }
}
