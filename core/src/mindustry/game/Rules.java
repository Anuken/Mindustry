package mindustry.game;

import mindustry.annotations.Annotations.*;
import arc.struct.*;
import arc.graphics.*;
import mindustry.content.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;

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
    public boolean unitDrops = true;
    /** Whether reactors can explode and damage other blocks. */
    public boolean reactorExplosions = true;
    /** How fast unit pads build units. */
    public float unitBuildSpeedMultiplier = 1f;
    /** How much health units start with. */
    public float unitHealthMultiplier = 1f;
    /** How much health players start with. */
    public float playerHealthMultiplier = 1f;
    /** How much health blocks start with. */
    public float blockHealthMultiplier = 1f;
    /** How much damage player mechs deal. */
    public float playerDamageMultiplier = 1f;
    /** How much damage any other units deal. */
    public float unitDamageMultiplier = 1f;
    /** Multiplier for buildings for the player. */
    public float buildCostMultiplier = 1f;
    /** Multiplier for building speed. */
    public float buildSpeedMultiplier = 1f;
    /** Multiplier for percentage of materials refunded when deconstructing */
    public float deconstructRefundMultiplier = 0.5f;
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
    /** Determinates if gamemode is attack mode */
    public boolean attackMode = false;
    /** Whether this is the editor gamemode. */
    public boolean editor = false;
    /** Whether the tutorial is enabled. False by default. */
    public boolean tutorial = false;
    /** Whether a gameover can happen at all. Set this to false to implement custom gameover conditions. */
    public boolean canGameOver = true;
    /** Starting items put in cores */
    public Array<ItemStack> loadout = Array.with(ItemStack.with(Items.copper, 100));
    /** Blocks that cannot be placed. */
    public ObjectSet<Block> bannedBlocks = new ObjectSet<>();
    /** Whether everything is dark. Enables lights. Experimental. */
    public boolean lighting = false;
    /** Ambient light color, used when lighting is enabled. */
    public Color ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.99f);
    /** Multiplier for solar panel power output.
    negative = use ambient light if lighting is enabled. */
    public float solarPowerMultiplier = -1f;
    /** team of the player by default */
    public Team defaultTeam = Team.sharded;
    /** team of the enemy in waves/sectors */
    public Team waveTeam = Team.crux;
    /** special tags for additional info */
    public StringMap tags = new StringMap();

    /** Copies this ruleset exactly. Not very efficient at all, do not use often. */
    public Rules copy(){
        return JsonIO.copy(this);
    }

    /** Returns the gamemode that best fits these rules.*/
    public Gamemode mode(){
        return Gamemode.bestFit(this);
    }
}
