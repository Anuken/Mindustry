package io.anuke.mindustry.game;

import io.anuke.ucore.util.Bundles;

public enum GameMode{
    waves,
	sandbox{{
        infiniteResources = true;
        disableWaveTimer = true;
    }},
    freebuild{{
        disableWaveTimer = true;
    }},
    noWaves{{
        disableWaves = true;
        hidden = true;
        autoSpawn = true;
    }},
    pvp{{
        disableWaves = true;
        isPvp = true;
        hidden = true;
    }};

    public boolean infiniteResources, disableWaveTimer, disableWaves, hidden, autoSpawn, isPvp;

    public String description(){
        return Bundles.get("mode." + name() + ".description");
    }

    @Override
    public String toString(){
        return Bundles.get("mode." + name() + ".name");
    }

}
