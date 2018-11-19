package io.anuke.mindustry.ui;

import io.anuke.mindustry.type.Liquid;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Table;

/**An ItemDisplay, but for liquids.*/
public class LiquidDisplay extends Table{

    public LiquidDisplay(Liquid liquid){
        add(new Image(liquid.getContentIcon())).size(8*3);
        add(liquid.localizedName()).padLeft(3);
    }
}
