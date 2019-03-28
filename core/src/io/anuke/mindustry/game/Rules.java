package io.anuke.mindustry.game;

import io.anuke.annotations.Annotations.Serialize;
import io.anuke.arc.collection.Array;

/**Defines current rules on how the game should function.
 * Does not store game state, just configuration.*/
@Serialize
public class Rules{
    /**Whether the player has infinite resources.*/
    public boolean infiniteResources;
    /**Whether the waves come automatically on a timer. If not, waves come when the play button is pressed.*/
    public boolean waveTimer = true;
    /**Whether waves are spawnable at all.*/
    public boolean waves;
    /**Whether the enemy AI has infinite resources in most of their buildings and turrets.*/
    public boolean enemyCheat;
    /**Whether the game objective is PvP. Note that this enables automatic hosting.*/
    public boolean pvp;
    /**Whether enemy units drop random items on death.*/
    public boolean unitDrops;
    /**Multiplier for buildings for the player.*/
    public float buildCostMultiplier = 1f;
    /**Multiplier for building speed.*/
    public float buildSpeedMultiplier = 1f;
    /**No-build zone around enemy core radius.*/
    public float enemyCoreBuildRadius = 400f;
    /**Player respawn time in ticks.*/
    public float respawnTime = 60 * 4;
    /**Time between waves in ticks.*/
    public float waveSpacing = 60 * 60 * 2;
    /**Zone ID, -1 for invalid zone.*/
    public byte zone = -1;
    /**Spawn layout. Since only zones modify this, it should be assigned on save load.*/
    public transient Array<SpawnGroup> spawns = DefaultWaves.get();
}
