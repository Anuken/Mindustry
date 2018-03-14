package io.anuke.mindustry.game;

import com.badlogic.gdx.graphics.Color;

public enum Team {
    none(Color.GRAY),
    blue(Color.BLUE),
    red(Color.RED);

    public final Color color;

    Team(Color color){
        this.color = color;
    }
}
