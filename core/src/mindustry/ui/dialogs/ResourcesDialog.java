package mindustry.ui.dialogs;

import arc.graphics.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class ResourcesDialog extends BaseDialog{

    public ResourcesDialog(){
        super("//TODO resources");
        shown(this::setup);
        addCloseButton();
    }

    void setup(){
        cont.clear();

        cont.table(Tex.button,  t -> {
            t.left();
            t.margin(10f);
            int[] exports = universe.getTotalExports();
            for(Item item : content.items()){
                if(exports[item.id] > 0 || data.getItem(item) > 0){
                    t.image(item.icon(Cicon.small)).padRight(4);
                    t.add(ui.formatAmount(data.getItem(item))).color(Color.lightGray);
                    if(exports[item.id] > 0){
                        t.add("+ [accent]" + ui.formatAmount(exports[item.id]) + " [lightgray]/T");
                    }else{
                        t.add();
                    }
                    t.row();
                }
            }
        });


    }
}
