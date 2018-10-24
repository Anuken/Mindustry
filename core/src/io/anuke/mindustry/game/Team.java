package io.anuke.mindustry.game;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.util.Bundles;

public enum Team{
    none(Color.valueOf("4d4e58")),
    blue(Color.ROYAL),
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
        return Bundles.get("team." + name() + ".name");
    }
}
