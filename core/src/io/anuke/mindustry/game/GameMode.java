package io.anuke.mindustry.game;

import io.anuke.arc.util.Bundles;

public enum GameMode{
    waves,
	sandbox{{
        infiniteResources = true;
        disableWaveTimer = true;
    }},
    freebuild{{
        disableWaveTimer = true;
    }},
    attack{{
        disableWaves = true;
        enemyCheat = true;
    }},
    victory{{
        disableWaves = true;
        hidden = true;
        enemyCheat = false;
        showMission = false;
    }},
    pvp{{
        disableWaves = true;
        isPvp = true;
        enemyCoreBuildRadius = 600f;
        respawnTime = 60 * 10;
    }};

    public boolean infiniteResources, disableWaveTimer, disableWaves, showMission = true, hidden, enemyCheat, isPvp;
    public float enemyCoreBuildRadius = 400f;
    public float respawnTime = 60 * 4;

    public String description(){
        return Core.bundle.get("mode." + name() + ".description");
    }

    @Override
    public String toString(){
        return Core.bundle.get("mode." + name() + ".name");
    }

}
