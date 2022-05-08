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
    /** Whether the player's team won the match. */
    public boolean won = false;
    /** If true, the server has been put into the paused state on multiplayer. This is synced. */
    public boolean serverPaused = false;
    /** Server ticks/second. Only valid in multiplayer. */
    public int serverTps = -1;
    /** Map that is currently being played on. */
    public Map map = emptyMap;
    /** The current game rules. */
    public Rules rules = new Rules();
    /** Statistics for this save/game. Displayed after game over. */
    public GameStats stats = new GameStats();
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
        //cannot pause when in multiplayer
        if(astate == State.paused && net.active()) return;

        Events.fire(new StateChangeEvent(state, astate));
        state = astate;
    }

    public boolean hasSpawns(){
        return rules.waves && !(isCampaign() && rules.attackMode);
    }

    /** Note that being in a campaign does not necessarily mean having a sector. */
    public boolean isCampaign(){
        return rules.sector != null;
    }

    public boolean hasSector(){
        return rules.sector != null;
    }

    @Nullable
    public Sector getSector(){
        return rules.sector;
    }

    public boolean isEditor(){
        return rules.editor;
    }

    public boolean isPaused(){
        return (is(State.paused) && !net.active()) || (serverPaused && !isMenu());
    }

    public boolean isPlaying(){
        return (state == State.playing) || (state == State.paused && !isPaused());
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
