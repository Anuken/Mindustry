package mindustry.core;

import arc.*;
import arc.util.ArcAnnotate.*;
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
    /** Whether the game is in game over state. */
    public boolean gameOver = false, launched = false, serverPaused = false;
    /** Map that is currently being played on. */
    public @NonNull Map map = emptyMap;
    /** The current game rules. */
    public Rules rules = new Rules();
    /** Statistics for this save/game. Displayed after game over. */
    public Stats stats = new Stats();
    /** Global attributes of the environment, calculated by weather. */
    public Attributes envAttrs = new Attributes();
    /** Sector information. Only valid in the campaign. */
    public SectorInfo secinfo = new SectorInfo();
    /** Team data. Gets reset every new game. */
    public Teams teams = new Teams();
    /** Number of enemies in the game; only used clientside in servers. */
    public int enemies;
    /** Current game state. */
    private State state = State.menu;

    //TODO optimize
    public Unit boss(){
        return Groups.unit.find(u -> u.isBoss() && u.team == rules.waveTeam);
    }

    public void set(State astate){
        Events.fire(new StateChangeEvent(state, astate));
        state = astate;
    }

    /** Note that being in a campaign does not necessarily mean having a sector. */
    public boolean isCampaign(){
        return rules.sector != null;
    }

    /** @return whether the player is in a campaign and they are out of sector time */
    public boolean isOutOfTime(){
        return isCampaign() && isGame() && getSector().getTimeSpent() >= turnDuration;
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
        return (is(State.paused) && !net.active()) || (gameOver && !net.active()) || (serverPaused && !isMenu());
    }

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
