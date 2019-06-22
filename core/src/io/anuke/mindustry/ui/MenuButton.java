package io.anuke.mindustry.ui;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.util.Align;

import static io.anuke.mindustry.Vars.iconsize;

public class MenuButton extends TextButton{

    public MenuButton(String icon, String text, Runnable clicked){
        this(icon, text, null, clicked);
    }

    public MenuButton(String icon, String text, String description, Runnable clicked){
        super("default");

        clicked(clicked);
        clearChildren();

        margin(0);

        table(t -> {
            t.addImage(icon).size(iconsize).padLeft(6);

            t.add(text).wrap().growX().get().setAlignment(Align.center, Align.left);
            if(description != null){
                t.row();
                t.add(description).color(Color.LIGHT_GRAY);
            }
        }).padLeft(5).growX();
    }
}
