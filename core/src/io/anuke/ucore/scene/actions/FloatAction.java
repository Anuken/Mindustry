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

/** An action that has a float, whose value is transitioned over time.
 * @author Nathan Sweet */
public class FloatAction extends TemporalAction {
	private float start, end;
	private float value;

	/** Creates a FloatAction that transitions from 0 to 1. */
	public FloatAction () {
		start = 0;
		end = 1;
	}

	/** Creates a FloatAction that transitions from start to end. */
	public FloatAction (float start, float end) {
		this.start = start;
		this.end = end;
	}

	protected void begin () {
		value = start;
	}

	protected void update (float percent) {
		value = start + (end - start) * percent;
	}

	/** Gets the current float value. */
	public float getValue () {
		return value;
	}

	/** Sets the current float value. */
	public void setValue (float value) {
		this.value = value;
	}

	public float getStart () {
		return start;
	}

	/** Sets the value to transition from. */
	public void setStart (float start) {
		this.start = start;
	}

	public float getEnd () {
		return end;
	}

	/** Sets the value to transition to. */
	public void setEnd (float end) {
		this.end = end;
	}
}
