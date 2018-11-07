package io.anuke.mindustry;

import com.badlogic.gdx.Game;

public class MindustryGame extends Game {
    @Override
    public void create() {
        setScreen(new SplashScreen(this));
    }
}
