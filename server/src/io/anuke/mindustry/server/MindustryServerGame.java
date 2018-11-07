package io.anuke.mindustry.server;

import com.badlogic.gdx.Game;

public class MindustryServerGame extends Game {
    private String[] args;

    public MindustryServerGame(String[] args) {
        this.args = args;
    }

    @Override
    public void create() {
        setScreen(new MindustryServer(args));
    }


}
