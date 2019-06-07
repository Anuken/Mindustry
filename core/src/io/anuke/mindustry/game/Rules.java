package io.anuke.mindustry.game;

import io.anuke.annotations.Annotations.Serialize;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.io.JsonIO;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Zone;

/**
 * Defines current rules on how the game should function.
 * Does not store game state, just configuration.
 */
@Serialize
public class Rules{
    /** Whether the player has infinite resources. */
    public boolean infiniteResources;
    /** Whether the waves come automatically on a timer. If not, waves come when the play button is pressed. */
    public boolean waveTimer = true;
    /** Whether waves are spawnable at all. */
    public boolean waves;
    /** Whether the enemy AI has infinite resources in most of their buildings and turrets. */
    public boolean enemyCheat;
    /** Whether the game objective is PvP. Note that this enables automatic hosting. */
    public boolean pvp;
    /** Whether enemy units drop random items on death. */
    public boolean unitDrops;
    /** How fast unit pads build units. */
    public float unitBuildSpeedMultiplier = 1f;
    /** How much health units start with. */
    public float unitHealthMultiplier = 1f;
    /** How much health players start with. */
    public float playerHealthMultiplier = 1f;
    /** How much damage player mechs deal. */
    public float playerDamageMultiplier = 1f;
    /** How much damage any other units deal. */
    public float unitDamageMultiplier = 1f;
    /** Multiplier for buildings for the player. */
    public float buildCostMultiplier = 1f;
    /** Multiplier for building speed. */
    public float buildSpeedMultiplier = 1f;
    /** No-build zone around enemy core radius. */
    public float enemyCoreBuildRadius = 400f;
    /** Radius around enemy wave drop zones.*/
    public float dropZoneRadius = 380f;
    /** Player respawn time in ticks. */
    public float respawnTime = 60 * 4;
    /** Time between waves in ticks. */
    public float waveSpacing = 60 * 60 * 2;
    /** How many times longer a boss wave takes. */
    public float bossWaveMultiplier = 3f;
    /** How many times longer a launch wave takes. */
    public float launchWaveMultiplier = 2f;
    /** Zone for saves that have them.*/
    public Zone zone;
    /** Spawn layout. Should be assigned on save load based on map or zone. */
    public Array<SpawnGroup> spawns = DefaultWaves.get();
    /** Determines if there should be limited respawns. */
    public boolean limitedRespawns = false;
    /** How many times player can respawn during one wave. */
    public int respawns = 5;
    /** Hold wave timer until all enemies are destroyed. */
    public boolean waitForWaveToEnd = false;
    /** Determinates if gamemode is attack mode */
    public boolean attackMode = false;
    /** Whether this is the editor gamemode. */
    public boolean editor = false;
    /** Items that the player starts with here. Not applicable to zones.*/
    public Array<ItemStack> startingItems = Array.with(new ItemStack(Items.copper, 200));

    /** Copies this ruleset exactly. Not very efficient at all, do not use often. */
    public Rules copy(){
        return JsonIO.read(Rules.class, JsonIO.write(this));
    }
}
