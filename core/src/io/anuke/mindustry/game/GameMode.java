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
        enemyCheat = true;
        showPads = true;
    }},
    victory{{
        disableWaves = true;
        hidden = true;
        enemyCheat = false;
        showPads = true;
        showMission = false;
    }},
    pvp{{
        showPads = true;
        disableWaves = true;
        isPvp = true;
        hidden = true;
        enemyCoreBuildRadius = 600f;
        respawnTime = 60 * 10;
    }};

    public boolean infiniteResources, disableWaveTimer, disableWaves, showMission = true, hidden, enemyCheat, isPvp, showPads;
    public float enemyCoreBuildRadius = 400f;
    public float respawnTime = 60 * 4;

    public String description(){
        return Bundles.get("mode." + name() + ".description");
    }

    @Override
    public String toString(){
        return Bundles.get("mode." + name() + ".name");
    }

}
