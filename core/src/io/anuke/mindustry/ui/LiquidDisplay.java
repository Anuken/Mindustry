package io.anuke.mindustry.ui;

import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Stack;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.type.Liquid;

/**An ItemDisplay, but for liquids.*/
public class LiquidDisplay extends Table{

    public LiquidDisplay(Liquid liquid){
        add(new Image(liquid.getContentIcon())).size(8*3);
        add(liquid.localizedName()).padLeft(3);
    }

    public LiquidDisplay(Liquid liquid, float amount){
        add(new Stack(){{
            add(new Image(liquid.getContentIcon()));

            if(amount != 0){
                Table t = new Table().left().bottom();
                t.add(Strings.toFixed(amount, 2));
                add(t);
            }
        }}).size(8*4);
        add(liquid.localizedName()).padLeft(3);
    }
}
