package io.anuke.ucore.function;

import com.badlogic.gdx.utils.Pool.Poolable;

public class DelayRun implements Poolable{
	public float delay;
	public Callable run;
	public Callable finish;
	
	@Override
	public void reset(){
		delay = 0;
		run = finish = null;
	}
}
