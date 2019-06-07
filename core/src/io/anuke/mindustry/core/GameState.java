package io.anuke.mindustry.core;

import io.anuke.arc.Events;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.game.EventType.StateChangeEvent;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.net.Net;

import static io.anuke.mindustry.Vars.unitGroups;
import static io.anuke.mindustry.Vars.waveTeam;

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
        return Net.client() ? enemies : unitGroups[waveTeam.ordinal()].size();
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
        return (is(State.paused) && !Net.active()) || (gameOver && !Net.active());
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
