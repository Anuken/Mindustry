/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.anuke.ucore.scene.actions;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Pool;

import io.anuke.ucore.scene.Action;

/** Base class for actions that transition over time using the percent complete.
 * @author Nathan Sweet */
abstract public class TemporalAction extends Action {
	private float duration, time;
	private Interpolation interpolation;
	private boolean reverse, began, complete;

	public TemporalAction () {
	}

	public TemporalAction (float duration) {
		this.duration = duration;
	}

	public TemporalAction (float duration, Interpolation interpolation) {
		this.duration = duration;
		this.interpolation = interpolation;
	}

	public boolean act (float delta) {
		if (complete) return true;
		Pool pool = getPool();
		setPool(null); // Ensure this action can't be returned to the pool while executing.
		try {
			if (!began) {
				begin();
				began = true;
			}
			time += delta;
			complete = time >= duration;
			float percent;
			if (complete)
				percent = 1;
			else {
				percent = time / duration;
				if (interpolation != null) percent = interpolation.apply(percent);
			}
			update(reverse ? 1 - percent : percent);
			if (complete) end();
			return complete;
		} finally {
			setPool(pool);
		}
	}

	/** Called the first time {@link #act(float)} is called. This is a good place to query the {@link #actor actor's} starting
	 * state. */
	protected void begin () {
	}

	/** Called the last time {@link #act(float)} is called. */
	protected void end () {
	}

	/** Called each frame.
	 * @param percent The percentage of completion for this action, growing from 0 to 1 over the duration. If
	 *           {@link #setReverse(boolean) reversed}, this will shrink from 1 to 0. */
	abstract protected void update (float percent);

	/** Skips to the end of the transition. */
	public void finish () {
		time = duration;
	}

	public void restart () {
		time = 0;
		began = false;
		complete = false;
	}

	public void reset () {
		super.reset();
		reverse = false;
		interpolation = null;
	}

	/** Gets the transition time so far. */
	public float getTime () {
		return time;
	}

	/** Sets the transition time so far. */
	public void setTime (float time) {
		this.time = time;
	}

	public float getDuration () {
		return duration;
	}

	/** Sets the length of the transition in seconds. */
	public void setDuration (float duration) {
		this.duration = duration;
	}

	public Interpolation getInterpolation () {
		return interpolation;
	}

	public void setInterpolation (Interpolation interpolation) {
		this.interpolation = interpolation;
	}

	public boolean isReverse () {
		return reverse;
	}

	/** When true, the action's progress will go from 100% to 0%. */
	public void setReverse (boolean reverse) {
		this.reverse = reverse;
	}
}
