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
    pvp{{
        showPads = true;
        disableWaves = true;
        isPvp = true;
        hidden = true;
        enemyCoreBuildRadius = 600f;
        enemyCoreShieldRadius = 0f;
        respawnTime = 60 * 10;
    }};

    public boolean infiniteResources, disableWaveTimer, disableWaves, hidden, enemyCheat, isPvp, showPads;
    public float enemyCoreBuildRadius = 400f;
    public float enemyCoreShieldRadius = 140f;
    public float respawnTime = 60 * 4;

    public String description(){
        return Bundles.get("mode." + name() + ".description");
    }

    @Override
    public String toString(){
        return Bundles.get("mode." + name() + ".name");
    }

}
