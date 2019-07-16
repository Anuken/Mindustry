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
    public float dropZoneRadius = 300f;
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
    /** Spawn layout. */
    public Array<SpawnGroup> spawns = new Array<>();
    /** Determines if there should be limited respawns. */
    public boolean limitedRespawns = false;
    /** How many times player can respawn during one wave. */
    public int respawns = 5;
    /** Hold wave timer until all enemies are destroyed. */
    public boolean waitForWaveToEnd = false;
    /** Determines if gamemode is attack mode */
    public boolean attackMode = false;
    /** Whether this is the editor gamemode. */
    public boolean editor = false;
    /** Starting items put in cores */
    public Array<ItemStack> loadout = Array.with(ItemStack.with(Items.copper, 200));

    // resources war
    /** Determines if gamemode is resourceswar mode */
    public boolean resourcesWar = false;
    /** Determines how often weakest team should be eliminated in ticks */
    public float eliminationTime = 300 * 60;
    /** Determines if resourceswar is in rushmode */
    public boolean rushGame = false;
    /** First points threshold */
    public int firstThreshold = 2000;
    /** How points threshold is incremented */
    public int bumpThreshold = 1000;
    /** Are random buffs enabled */
    public boolean buffing = true;
    /** How long buff should lasts in ticks */
    public float buffTime = 60 * 60;
    /** Time between buffs in ticks */
    public float buffSpacing = 60 * 60;
    /** How activated buff multiplies value of consumed item */
    public float buffMultiplier = 2f;
    /** If false, the weakest player gets eliminated. If true, the weakest team lost one life, and the best gains one*/
    public boolean enableLifes = true;
    /** How many lifes every team has on start */
    public int lifesCount = 2;

    /** Copies this ruleset exactly. Not very efficient at all, do not use often. */
    public Rules copy(){
        return JsonIO.copy(this);
    }
}
