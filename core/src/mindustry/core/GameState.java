package mindustry.core;

import arc.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class GameState{
    /** Current wave number, can be anything in non-wave modes. */
    public int wave = 1;
    /** Wave countdown in ticks. */
    public float wavetime;
    /** Logic tick. */
    public double tick;
    /** Continuously ticks up every non-paused update. */
    public long updateId;
    /** Whether the game is in game over state. */
    public boolean gameOver = false;
    /** For the campaign, this is whether the map is in a "after game over" state. In this state, the game is always paused. */
    public boolean afterGameOver = false;
    /** Whether the player's team won the match. */
    public boolean won = false;
    /** Server ticks/second. Only valid in multiplayer. */
    public int serverTps = -1;
    /** Map that is currently being played on. */
    public Map map = emptyMap;
    /** The current game rules. */
    public Rules rules = new Rules();
    /** Statistics for this save/game. Displayed after game over. */
    public GameStats stats = new GameStats();
    /** Markers not linked to objectives. Controlled by world processors. */
    public MapMarkers markers = new MapMarkers();
    /** Locale-specific string bundles of current map */
    public MapLocales mapLocales = new MapLocales();
    /** Global attributes of the environment, calculated by weather. */
    public Attributes envAttrs = new Attributes();
    /** Team data. Gets reset every new game. */
    public Teams teams = new Teams();
    /** Number of enemies in the game; only used clientside in servers. */
    public int enemies;
    /** Map being playtested (not edited!) */
    public @Nullable Map playtestingMap;
    /** Current game state. */
    private State state = State.menu;

    @Nullable
    public Unit boss(){
        return teams.bosses.firstOpt();
    }

    public void set(State astate){
        //nothing to change.
        if(state == astate) return;

        Events.fire(new StateChangeEvent(state, astate));
        state = astate;
    }

    public boolean hasSpawns(){
        return rules.waves && ((rules.waveTeam.cores().size > 0 && rules.attackMode) || rules.spawns.size > 0);
    }

    /** Note that being in a campaign does not necessarily mean having a sector. */
    public boolean isCampaign(){
        return rules.sector != null;
    }

    public boolean hasSector(){
        return rules.sector != null;
    }

    public @Nullable Sector getSector(){
        return rules.sector;
    }

    public @Nullable Planet getPlanet(){
        return rules.sector != null ? rules.sector.planet : rules.planet;
    }

    public boolean isEditor(){
        return rules.editor;
    }

    public boolean isPaused(){
        return state == State.paused;
    }

    /** @return whether there is an unpaused game in progress. */
    public boolean isPlaying(){
        return state == State.playing;
    }

    /** @return whether the current state is *not* the menu. */
    public boolean isGame(){
        return state != State.menu;
    }

    public boolean isMenu(){
        return state == State.menu;
    }

    public boolean is(State astate){
        return state == astate;
    }

    public State getState(){
        return state;
    }

    public enum State{
        paused, playing, menu
    }
}
