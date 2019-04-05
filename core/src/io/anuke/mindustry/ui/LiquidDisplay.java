package io.anuke.mindustry.ui;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Stack;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.meta.StatUnit;

/**An ItemDisplay, but for liquids.*/
public class LiquidDisplay extends Table{

    public LiquidDisplay(Liquid liquid, float amount, boolean perSecond){
        add(new Stack(){{
            add(new Image(liquid.getContentIcon()));

            if(amount != 0){
                Table t = new Table().left().bottom();
                t.add(Strings.autoFixed(amount, 1));
                add(t);
            }
        }}).size(8*4).padRight(3);

        if(perSecond){
            add(StatUnit.perSecond.localized()).padLeft(2).padRight(5).color(Color.LIGHT_GRAY);
        }

        add(liquid.localizedName());
    }
}
