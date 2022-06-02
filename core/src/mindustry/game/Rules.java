package mindustry.game;

import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.MapObjectives.*;
import mindustry.graphics.g3d.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

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
    /** Determines if gamemode is attack mode. */
    public boolean attackMode = false;
    /** Whether this is the editor gamemode. */
    public boolean editor = false;
    /** Whether a gameover can happen at all. Set this to false to implement custom gameover conditions. */
    public boolean canGameOver = true;
    /** Whether cores change teams when they are destroyed. */
    public boolean coreCapture = false;
    /** Whether reactors can explode and damage other blocks. */
    public boolean reactorExplosions = true;
    /** Whether schematics are allowed. */
    public boolean schematicsAllowed = true;
    /** Whether friendly explosions can occur and set fire/damage other blocks. */
    public boolean damageExplosions = true;
    /** Whether fire is enabled. */
    public boolean fire = true;
    /** Whether units use and require ammo. */
    public boolean unitAmmo = false;
    /** EXPERIMENTAL! If true, blocks will update in units and share power. */
    public boolean unitPayloadUpdate = false;
    /** Whether cores add to unit limit */
    public boolean unitCapVariable = true;
    /** If true, unit spawn points are shown. */
    public boolean showSpawns = false;
    /** How fast unit factories build units. */
    public float unitBuildSpeedMultiplier = 1f;
    /** How much damage any other units deal. */
    public float unitDamageMultiplier = 1f;
    /** Whether to allow units to build with logic. */
    public boolean logicUnitBuild = true;
    /** If true, world processors no longer update. Used for testing. */
    public boolean disableWorldProcessors = false;
    /** If true, world processors are always accessible in the game. */
    public boolean accessibleWorldLogic = false;
    /** How much health blocks start with. */
    public float blockHealthMultiplier = 1f;
    /** How much damage blocks (turrets) deal. */
    public float blockDamageMultiplier = 1f;
    /** Multiplier for buildings resource cost. */
    public float buildCostMultiplier = 1f;
    /** Multiplier for building speed. */
    public float buildSpeedMultiplier = 1f;
    /** Multiplier for percentage of materials refunded when deconstructing. */
    public float deconstructRefundMultiplier = 0.5f;
    /** No-build zone around enemy core radius. */
    public float enemyCoreBuildRadius = 400f;
    /** If true, no-build zones are calculated based on the closest core. */
    public boolean polygonCoreProtection = false;
    /** If true, blocks cannot be placed near blocks that are near the enemy team.*/
    public boolean placeRangeCheck = false;
    /** If true, dead teams in PvP automatically have their blocks & units converted to derelict upon death. */
    public boolean cleanupDeadTeams = true;
    /** If true, items can only be deposited in the core. */
    public boolean onlyDepositCore = false;
    /** If true, every enemy block in the radius of the (enemy) core is destroyed upon death. Used for campaign maps. */
    public boolean coreDestroyClear = false;
    /** Radius around enemy wave drop zones.*/
    public float dropZoneRadius = 300f;
    /** Time between waves in ticks. */
    public float waveSpacing = 2 * Time.toMinutes;
    /** Starting wave spacing; if <0, uses waveSpacing * 2. */
    public float initialWaveSpacing = 0f;
    /** Wave after which the player 'wins'. Used in sectors. Use a value <= 0 to disable. */
    public int winWave = 0;
    /** Base unit cap. Can still be increased by blocks. */
    public int unitCap = 0;
    /** Environment drag multiplier. */
    public float dragMultiplier = 1f;
    /** Environmental flags that dictate visuals & how blocks function. */
    public int env = Vars.defaultEnv;
    /** Attributes of the environment. */
    public Attributes attributes = new Attributes();
    /** Sector for saves that have them. */
    public @Nullable Sector sector;
    /** Spawn layout. */
    public Seq<SpawnGroup> spawns = new Seq<>();
    /** Starting items put in cores. */
    public Seq<ItemStack> loadout = ItemStack.list(Items.copper, 100);
    /** Weather events that occur here. */
    public Seq<WeatherEntry> weather = new Seq<>(1);
    /** Blocks that cannot be placed. */
    public ObjectSet<Block> bannedBlocks = new ObjectSet<>();
    /** Units that cannot be built. */
    public ObjectSet<UnitType> bannedUnits = new ObjectSet<>();
    /** Reveals blocks normally hidden by build visibility. */
    public ObjectSet<Block> revealedBlocks = new ObjectSet<>();
    /** Unlocked content names. Only used in multiplayer when the campaign is enabled. */
    public ObjectSet<String> researched = new ObjectSet<>();
    /** Block containing these items as requirements are hidden. */
    public ObjectSet<Item> hiddenBuildItems = Items.erekirOnlyItems.asSet();
    /** Campaign-only map objectives. */
    public Seq<MapObjective> objectives = new Seq<>();
    /** Flags set by objectives. Used in world processors. n*/
    public ObjectSet<String> objectiveFlags = new ObjectSet<>();
    /** HIGHLY UNSTABLE/EXPERIMENTAL. DO NOT USE THIS. */
    public boolean fog = false;
    /** If fog = true, this is whether static (black) fog is enabled. */
    public boolean staticFog = true;
    /** Color for static, undiscovered fog of war areas. */
    public Color staticColor = new Color(0f, 0f, 0f, 1f);
    /** Color for discovered but un-monitored fog of war areas. */
    public Color dynamicColor = new Color(0f, 0f, 0f, 0.5f);
    /** Whether ambient lighting is enabled. */
    public boolean lighting = false;
    /** Ambient light color, used when lighting is enabled. */
    public Color ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.99f);
    /** team of the player by default. */
    public Team defaultTeam = Team.sharded;
    /** team of the enemy in waves/sectors. */
    public Team waveTeam = Team.crux;
    /** color of clouds that is displayed when the player is landing */
    public Color cloudColor = new Color(0f, 0f, 0f, 0f);
    /** name of the custom mode that this ruleset describes, or null. */
    public @Nullable String modeName;
    /** Mission string displayed instead of wave/core counter. Null to disable. */
    public @Nullable String mission;
    /** Whether cores incinerate items when full, just like in the campaign. */
    public boolean coreIncinerates = false;
    /** If false, borders fade out into darkness. Only use with custom backgrounds!*/
    public boolean borderDarkness = true;
    /** If true, the map play area is cropped based on the rectangle below. */
    public boolean limitMapArea = false;
    /** Map area limit rectangle. */
    public int limitX, limitY, limitWidth = 1, limitHeight = 1;
    /** If true, blocks outside the map area are disabled. */
    public boolean disableOutsideArea = true;
    /** special tags for additional info. */
    public StringMap tags = new StringMap();
    /** Name of callback to call for background rendering in mods; see Renderer#addCustomBackground. Runs last. */
    public @Nullable String customBackgroundCallback;
    /** path to background texture with extension (e.g. "sprites/space.png")*/
    public @Nullable String backgroundTexture;
    /** background texture move speed scaling - bigger numbers mean slower movement. 0 to disable. */
    public float backgroundSpeed = 27000f;
    /** background texture scaling factor */
    public float backgroundScl = 1f;
    /** background UV offsets */
    public float backgroundOffsetX = 0.1f, backgroundOffsetY = 0.1f;
    /** Parameters for planet rendered in the background. Cannot be changed once a map is loaded. */
    public @Nullable PlanetParams planetBackground;

    /** Copies this ruleset exactly. Not efficient at all, do not use often. */
    public Rules copy(){
        return JsonIO.copy(this);
    }

    /** Returns the gamemode that best fits these rules. */
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

    public boolean hasEnv(int env){
        return (this.env & env) != 0;
    }

    public float unitBuildSpeed(Team team){
        return unitBuildSpeedMultiplier * teams.get(team).unitBuildSpeedMultiplier;
    }

    public float unitDamage(Team team){
        return unitDamageMultiplier * teams.get(team).unitDamageMultiplier;
    }

    public float blockHealth(Team team){
        return blockHealthMultiplier * teams.get(team).blockHealthMultiplier;
    }

    public float blockDamage(Team team){
        return blockDamageMultiplier * teams.get(team).blockDamageMultiplier;
    }

    public float buildSpeed(Team team){
        return buildSpeedMultiplier * teams.get(team).buildSpeedMultiplier;
    }

    /** A team-specific ruleset. */
    public static class TeamRule{
        /** Whether, when AI is enabled, ships should be spawned from the core. TODO remove / unnecessary? */
        public boolean aiCoreSpawn = true;
        /** If true, blocks don't require power or resources. */
        public boolean cheat;
        /** If true, resources are not consumed when building. */
        public boolean infiniteResources;
        /** If true, this team has infinite unit ammo. */
        public boolean infiniteAmmo;

        /** Enables "RTS" unit AI. TODO wip */
        public boolean rtsAi;
        /** Minimum size of attack squads. */
        public int rtsMinSquad = 4;
        /** Minimum "advantage" needed for a squad to attack. Higher -> more cautious. */
        public float rtsMinWeight = 1.2f;

        /** How fast unit factories build units. */
        public float unitBuildSpeedMultiplier = 1f;
        /** How much damage any other units deal. */
        public float unitDamageMultiplier = 1f;
        /** How much health blocks start with. */
        public float blockHealthMultiplier = 1f;
        /** How much damage blocks (turrets) deal. */
        public float blockDamageMultiplier = 1f;
        /** Multiplier for building speed. */
        public float buildSpeedMultiplier = 1f;

        //build cost disabled due to technical complexity
    }

    /** A simple map for storing TeamRules in an efficient way without hashing. */
    public static class TeamRules implements JsonSerializable{
        final TeamRule[] values = new TeamRule[Team.all.length];

        public TeamRule get(Team team){
            TeamRule out = values[team.id];
            return out == null ? (values[team.id] = new TeamRule()) : out;
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
