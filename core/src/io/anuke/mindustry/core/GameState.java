package io.anuke.mindustry.core;

import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType.StateChange;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Inventory;
import io.anuke.ucore.core.Events;

public class GameState{
	private State state = State.menu;

	public final Inventory inventory = new Inventory();

	int wave = 1;
	int lastUpdated = -1;
	float wavetime;
	float extrawavetime;
	int enemies = 0;
	boolean gameOver = false;
	GameMode mode = GameMode.waves;
	Difficulty difficulty = Difficulty.normal;
	boolean friendlyFire;
	
	public void set(State astate){
		//TODO update RPC handler
		Events.fire(StateChange.class, state, astate);
		state = astate;
	}
	
	public boolean is(State astate){
		return state == astate;
	}
	
	public enum State{
		paused, playing, menu
	}
}
