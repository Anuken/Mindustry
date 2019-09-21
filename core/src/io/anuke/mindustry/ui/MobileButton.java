package io.anuke.mindustry.ui;

import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.util.Align;

public class MobileButton extends ImageButton{

    public MobileButton(Drawable icon, String text, Runnable listener){
        super(icon);
        clicked(listener);
        row();
        add(text).growX().wrap().center().get().setAlignment(Align.center, Align.center);
    }
}
