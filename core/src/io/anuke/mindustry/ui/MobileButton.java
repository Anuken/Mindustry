package io.anuke.mindustry.ui;

import com.badlogic.gdx.utils.Align;
import io.anuke.ucore.scene.ui.ImageButton;

public class MobileButton extends ImageButton{

    public MobileButton(String icon, float isize, String text, Runnable listener){
        super(icon);
        resizeImage(isize);
        clicked(listener);
        row();
        add(text).growX().wrap().center().get().setAlignment(Align.center, Align.center);
    }
}
