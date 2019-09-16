package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.graphics.*;

public enum Team{
    derelict(Color.valueOf("4d4e58")),
    sharded(Pal.accent),
    crux(Color.valueOf("e82d2d")),
    green(Color.valueOf("4dd98b")),
    purple(Color.valueOf("9a4bdf")),
    blue(Color.royal.cpy());

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
