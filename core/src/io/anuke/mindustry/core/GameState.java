package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.type.base.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;

import static io.anuke.mindustry.Vars.*;

public class GameState{
    /** Current wave number, can be anything in non-wave modes. */
    public int wave = 1;
    /** Wave countdown in ticks. */
    public float wavetime;
    /** Whether the game is in game over state. */
    public boolean gameOver = false, launched = false;
    /** The current game rules. */
    public Rules rules = new Rules();
    /** Statistics for this save/game. Displayed after game over. */
    public Stats stats = new Stats();
    /** Team data. Gets reset every new game. */
    public Teams teams = new Teams();
    /** Number of enemies in the game; only used clientside in servers. */
    public int enemies;
    /** Current game state. */
    private State state = State.menu;

    public int enemies(){
        return net.client() ? enemies : unitGroups[waveTeam.ordinal()].count(b -> !(b instanceof BaseDrone));
    }

    public BaseUnit boss(){
        return unitGroups[waveTeam.ordinal()].find(BaseUnit::isBoss);
    }

    public void set(State astate){
        Events.fire(new StateChangeEvent(state, astate));
        state = astate;
    }

    public boolean isEditor(){
        return rules.editor;
    }

    public boolean isPaused(){
        return (is(State.paused) && !net.active()) || (gameOver && !net.active());
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
