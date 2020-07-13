package mindustry.game;

import arc.graphics.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.content.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.world.*;

/**
 * Defines current rules on how the game should function.
 * Does not store game state, just configuration.
 */
public class Rules{
    /** Sandbox mode: Enables infinite resources, build range and build speed. */
    public boolean infiniteResources;
    /** Team-specific rules. */
    public TeamRules teams = new TeamRules();
    /** Whether the waves come automatically on a timer. If not, waves come when the play button is pressed. */
    public boolean waveTimer = true;
    /** Whether waves are spawnable at all. */
    public boolean waves;
    /** Whether the game objective is PvP. Note that this enables automatic hosting. */
    public boolean pvp;
    /** Whether to pause the wave timer until all enemies are destroyed. */
    public boolean waitEnemies = false;
    /** Determinates if gamemode is attack mode */
    public boolean attackMode = false;
    /** Whether this is the editor gamemode. */
    public boolean editor = false;
    /** Whether the tutorial is enabled. False by default. */
    public boolean tutorial = false;
    /** Whether a gameover can happen at all. Set this to false to implement custom gameover conditions. */
    public boolean canGameOver = true;
    /** Whether reactors can explode and damage other blocks. */
    public boolean reactorExplosions = true;
    /** Whether units use and require ammo. */
    public boolean unitAmmo = false;
    /** How fast unit pads build units. */
    public float unitBuildSpeedMultiplier = 1f;
    /** How much health units start with. */
    public float unitHealthMultiplier = 1f;
    /** How much damage any other units deal. */
    public float unitDamageMultiplier = 1f;
    /** How much health blocks start with. */
    public float blockHealthMultiplier = 1f;
    /** How much damage blocks (turrets) deal. */
    public float blockDamageMultiplier = 1f;
    /** Multiplier for buildings resource cost. */
    public float buildCostMultiplier = 1f;
    /** Multiplier for building speed. */
    public float buildSpeedMultiplier = 1f;
    /** Multiplier for percentage of materials refunded when deconstructing */
    public float deconstructRefundMultiplier = 0.5f;
    /** No-build zone around enemy core radius. */
    public float enemyCoreBuildRadius = 400f;
    /** Radius around enemy wave drop zones.*/
    public float dropZoneRadius = 300f;
    /** Time between waves in ticks. */
    public float waveSpacing = 60 * 60 * 2;
    /** How many times longer a launch wave takes. */
    public float launchWaveMultiplier = 2f;
    /** Wave after which the player 'wins'. Used in sectors. Use a value <= 0 to disable. */
    public int winWave = 0;
    /** Base unit cap. Can still be increased by blocks. */
    public int unitCap = 0;
    /** Sector for saves that have them.*/
    public @Nullable Sector sector;
    /** Spawn layout. */
    public Seq<SpawnGroup> spawns = new Seq<>();
    /** Starting items put in cores */
    public Seq<ItemStack> loadout = ItemStack.list(Items.copper, 100);
    /** Weather events that occur here. */
    public Seq<WeatherEntry> weather = new Seq<>(1);
    /** Blocks that cannot be placed. */
    public ObjectSet<Block> bannedBlocks = new ObjectSet<>();
    /** Whether ambient lighting is enabled. */
    public boolean lighting = false;
    /** Whether enemy lighting is visible.
     * If lighting is enabled and this is false, a fog-of-war effect is partially achieved. */
    public boolean enemyLights = true;
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

    /** A team-specific ruleset. */
    public static class TeamRule{
        /** Whether to use building AI. */
        public boolean ai;
        /** TODO Tier of blocks/designs that the AI uses for building. [0, 1]*/
        public float aiTier = 0f;
        /** If true, blocks don't require power or resources. */
        public boolean cheat;
        /** If true, resources are not consumed when building. */
        public boolean infiniteResources;
        /** If true, this team has infinite unit ammo. */
        public boolean infiniteAmmo;
    }

    /** Copies this ruleset exactly. Not efficient at all, do not use often. */
    public Rules copy(){
        return JsonIO.copy(this);
    }

    /** Returns the gamemode that best fits these rules.*/
    public Gamemode mode(){
        if(pvp){
            return Gamemode.pvp;
        }else if(editor){
            return Gamemode.editor;
        }else if(attackMode){
            return Gamemode.attack;
        }else if(infiniteResources){
            return Gamemode.sandbox;
        }else{
            return Gamemode.survival;
        }
    }

    /** A simple map for storing TeamRules in an efficient way without hashing. */
    public static class TeamRules implements Serializable{
        final TeamRule[] values = new TeamRule[Team.all.length];

        public TeamRule get(Team team){
            TeamRule out = values[team.id];
            if(out == null) values[team.id] = (out = new TeamRule());
            return out;
        }

        @Override
        public void write(Json json){
            for(Team team : Team.all){
                if(values[team.id] != null){
                    json.writeValue(team.id + "", values[team.id], TeamRule.class);
                }
            }
        }

        @Override
        public void read(Json json, JsonValue jsonData){
            for(JsonValue value : jsonData){
                values[Integer.parseInt(value.name)] = json.readValue(TeamRule.class, value);
            }
        }
    }
}
