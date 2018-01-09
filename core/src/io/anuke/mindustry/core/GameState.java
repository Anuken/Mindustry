package io.anuke.mindustry.core;

import io.anuke.mindustry.Mindustry;
import io.anuke.ucore.core.Timers;

public class GameState{
	private static State state = State.menu;
	
	public static void set(State astate){

		if((astate == State.playing && state == State.menu) || (astate == State.menu && state != State.menu)){
			Timers.runTask(5f, Mindustry.platforms::updateRPC);
		}

		state = astate;
	}
	
	public static boolean is(State astate){
		return state == astate;
	}
	
	public enum State{
		paused, playing, menu
	}
}
