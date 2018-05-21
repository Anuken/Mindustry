package io.anuke.mindustry.entities;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.StatusController.TransitionResult;

public class StatusEffect{
	private static final Array<StatusEffect> array = new Array<>();
	private static int lastid;

	/**Duration of this status effect in ticks at maximum power.*/
	public final float baseDuration;
	public final int id;

	/**Set of 'opposite' effects, which will decrease the duration of this effect when applied.*/
	protected ObjectSet<StatusEffect> opposites = new ObjectSet<>();
	/**The strength of time decrease when met with an opposite effect, as a fraction of the other's duration.*/
	protected float oppositeScale = 0.5f;

	public StatusEffect(float baseDuration){
		this.baseDuration = baseDuration;

		id = lastid++;
		array.add(this);
	}

	/**Runs every tick on the affected unit while time is greater than 0.*/
	public void update(Unit unit, float time){}

	/**Called when transitioning between two status effects.
	 * @param to The state to transition to
	 * @param time The current status effect time
	 * @param newTime The time that the new status effect will last*/
	public TransitionResult getTransition(Unit unit, StatusEffect to, float time, float newTime, TransitionResult result){
		if(opposites.contains(to)){
			time -= newTime*oppositeScale;
			if(time > 0) {
				return result.set(this, time);
			}
		}

		return result.set(to, newTime);
	}

	/**Called when this effect transitions to a new status effect.*/
	public void onTransition(Unit unit, StatusEffect to){}

	public void setOpposites(StatusEffect... effects){
		for(StatusEffect e : effects){
			opposites.add(e);
		}
	}

	public static StatusEffect getByID(int id){
		return array.get(id);
	}

	public static Array<StatusEffect> getAllEffects(){
		return array;
	}
}
