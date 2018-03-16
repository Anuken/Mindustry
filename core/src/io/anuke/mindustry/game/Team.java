package io.anuke.mindustry.game;

import com.badlogic.gdx.graphics.Color;

public enum Team {
    none(Color.DARK_GRAY),
    blue(Color.ROYAL),
    red(Color.SCARLET);

    public final Color color;

    Team(Color color){
        this.color = color;
    }
}
