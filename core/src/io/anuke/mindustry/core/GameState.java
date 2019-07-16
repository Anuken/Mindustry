package io.anuke.mindustry.core;

import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.entities.type.base.BaseDrone;
import io.anuke.mindustry.game.EventType.StateChangeEvent;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;

import static io.anuke.mindustry.Vars.*;
public class GameState{
    /** Current wave number, can be anything in non-wave modes. */
    public int wave = 1;
    /** Wave countdown in ticks. */
    public float wavetime;
    /** Next elimination countdown in ticks */
    public float eliminationtime;
    /** Current round number, can be anything in non-resources-war modes */
    public int round = 1;
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
    /** Points. Used in resources-war mode */
    public int[] points = new int[Team.all.length];
    /** Points. Used in resources-war mode */
    public int pointsThreshold = rules.firstThreshold;
    /** Currently buffed item. Used in resources-war mode */
    public Item buffedItem = null;
    /** If there is buffed item set, keep time remain for buffed item. Else it counts time until next buff */
    public float buffTime;
    /** How many lifes team have, used in resources-war mode */
    public int[] lifes = new int[Team.all.length];

    public int enemies(){
        return Net.client() ? enemies : unitGroups[waveTeam.ordinal()].count(b -> !(b instanceof BaseDrone));
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

    public int points(Team team){
        return points[team.ordinal()];
    }
}
