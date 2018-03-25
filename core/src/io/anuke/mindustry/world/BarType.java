package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;

public enum BarType {
    health(Color.RED),
    inventory(Color.GREEN),
    power(Color.YELLOW),
    liquid(Color.ROYAL),
    heat(Color.CORAL),
    production(Color.PURPLE);

    public final Color color;

    BarType(Color color){
        this.color = color;
    }
}
