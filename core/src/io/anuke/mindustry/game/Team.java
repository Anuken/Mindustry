package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;

public enum Team{
    none(Color.valueOf("4d4e58")),
    blue(Color.valueOf("4169e1")),
    red(Color.valueOf("e84737")),
    green(Color.valueOf("1dc645")),
    purple(Color.valueOf("ba5bd9")),
    orange(Color.valueOf("e8c66a"));

    public final static Team[] all = values();
    public final Color color;
    public final int intColor;

    Team(Color color){
        this.color = color;
        intColor = Color.rgba8888(color);
    }

    public String localized(){
        return Core.bundle.get("team." + name() + ".name");
    }
}
