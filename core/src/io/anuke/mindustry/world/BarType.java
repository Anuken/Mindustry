package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;

public enum BarType{
    health(Color.SCARLET),
    inventory(Color.GREEN),
    power(Color.valueOf("fbeb67")),
    liquid(Color.ROYAL),
    heat(Color.CORAL),
    production(Color.valueOf("f4ba6e"));

    public final Color color;

    BarType(Color color){
        this.color = color;
    }
}
