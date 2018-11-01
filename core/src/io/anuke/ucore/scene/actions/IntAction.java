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

/** An action that has an int, whose value is transitioned over time.
 * @author Nathan Sweet */
public class IntAction extends TemporalAction {
	private int start, end;
	private int value;

	/** Creates an IntAction that transitions from 0 to 1. */
	public IntAction () {
		start = 0;
		end = 1;
	}

	/** Creates an IntAction that transitions from start to end. */
	public IntAction (int start, int end) {
		this.start = start;
		this.end = end;
	}

	protected void begin () {
		value = start;
	}

	protected void update (float percent) {
		value = (int)(start + (end - start) * percent);
	}

	/** Gets the current int value. */
	public int getValue () {
		return value;
	}

	/** Sets the current int value. */
	public void setValue (int value) {
		this.value = value;
	}

	public int getStart () {
		return start;
	}

	/** Sets the value to transition from. */
	public void setStart (int start) {
		this.start = start;
	}

	public int getEnd () {
		return end;
	}

	/** Sets the value to transition to. */
	public void setEnd (int end) {
		this.end = end;
	}
}
