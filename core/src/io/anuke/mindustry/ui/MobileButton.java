package io.anuke.mindustry.ui;

import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.ImageButton;

public class MobileButton extends ImageButton {

    public MobileButton(String icon, float isize, String text, Listenable listener) {
        super(icon);
        resizeImage(isize);
        clicked(listener);
        row();
        add(text);
    }
}
