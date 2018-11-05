package io.anuke.mindustry;

import com.badlogic.gdx.Game;

public class Mindustry extends Game {
    @Override
    public void create() {
        setScreen(new SplashScreen(this));
    }
}
