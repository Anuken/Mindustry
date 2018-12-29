package io.anuke.mindustry.ui;

import io.anuke.arc.util.Align;
import io.anuke.arc.scene.ui.ImageButton;

public class MobileButton extends ImageButton{

    public MobileButton(String icon, float isize, String text, Runnable listener){
        super(icon);
        resizeImage(isize);
        clicked(listener);
        row();
        add(text).growX().wrap().center().get().setAlignment(Align.center, Align.center);
    }
}
