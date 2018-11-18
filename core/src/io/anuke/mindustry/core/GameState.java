package io.anuke.mindustry.core;

import io.anuke.mindustry.ai.WaveSpawner;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType.StateChangeEvent;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Teams;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Events;

import static io.anuke.mindustry.Vars.unitGroups;
import static io.anuke.mindustry.Vars.waveTeam;

public class GameState{
    public int wave = 1;
    public float wavetime;
    public boolean gameOver = false;
    public GameMode mode = GameMode.waves;
    public Difficulty difficulty = Difficulty.normal;
    public WaveSpawner spawner = new WaveSpawner();
    public Teams teams = new Teams();
    public int enemies;
    private State state = State.menu;

    public int enemies(){
        return Net.client() ? enemies : unitGroups[waveTeam.ordinal()].size();
    }

    public void set(State astate){
        Events.fire(new StateChangeEvent(state, astate));
        state = astate;
    }

    public boolean isPaused(){
        return is(State.paused) && !Net.active();
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
