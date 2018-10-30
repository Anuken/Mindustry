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

/** Delays execution of an action or inserts a pause in a {@link SequenceAction}.
 * @author Nathan Sweet */
public class DelayAction extends DelegateAction {
	private float duration, time;

	public DelayAction () {
	}

	public DelayAction (float duration) {
		this.duration = duration;
	}

	protected boolean delegate (float delta) {
		if (time < duration) {
			time += delta;
			if (time < duration) return false;
			delta = time - duration;
		}
		if (action == null) return true;
		return action.act(delta);
	}

	/** Causes the delay to be complete. */
	public void finish () {
		time = duration;
	}

	public void restart () {
		super.restart();
		time = 0;
	}

	/** Gets the time spent waiting for the delay. */
	public float getTime () {
		return time;
	}

	/** Sets the time spent waiting for the delay. */
	public void setTime (float time) {
		this.time = time;
	}

	public float getDuration () {
		return duration;
	}

	/** Sets the length of the delay in seconds. */
	public void setDuration (float duration) {
		this.duration = duration;
	}
}
