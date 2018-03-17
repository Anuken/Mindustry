package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType.StateChangeEvent;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Inventory;
import io.anuke.mindustry.game.Team;
import io.anuke.ucore.core.Events;

public class GameState{
	private State state = State.menu;

	public final Inventory inventory = new Inventory();

	public int wave = 1;
	public int lastUpdated = -1;
	public float wavetime;
	public float extrawavetime;
	public int enemies = 0;
	public boolean gameOver = false;
	public GameMode mode = GameMode.waves;
	public Difficulty difficulty = Difficulty.normal;
	public boolean friendlyFire;
	public ObjectSet<Team> enemyTeams = new ObjectSet<>(), //enemies to the player team
			allyTeams = new ObjectSet<>(); //allies to the player team, includes the player team
	
	public void set(State astate){
		Events.fire(StateChangeEvent.class, state, astate);
		state = astate;
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
