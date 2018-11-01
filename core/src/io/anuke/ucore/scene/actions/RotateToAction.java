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

import com.badlogic.gdx.math.MathUtils;

/** Sets the actor's rotation from its current value to a specific value.
 * 
 * By default, the rotation will take you from the starting value to the specified value via simple subtraction. For example,
 * setting the start at 350 and the target at 10 will result in 340 degrees of movement.
 * 
 * If the action is instead set to useShortestDirection instead, it will rotate straight to the target angle, regardless of where
 * the angle starts and stops. For example, starting at 350 and rotating to 10 will cause 20 degrees of rotation.
 * 
 * @see com.badlogic.gdx.math.MathUtils#lerpAngleDeg(float, float, float)
 * 
 * @author Nathan Sweet */
public class RotateToAction extends TemporalAction {
	private float start, end;

	private boolean useShortestDirection = false;

	public RotateToAction () {
	}

	/** @param useShortestDirection Set to true to move directly to the closest angle */
	public RotateToAction (boolean useShortestDirection) {
		this.useShortestDirection = useShortestDirection;
	}

	@Override
	protected void begin () {
		start = target.getRotation();
	}

	@Override
	protected void update (float percent) {
		if (useShortestDirection)
			target.setRotation(MathUtils.lerpAngleDeg(this.start, this.end, percent));
		else
			target.setRotation(start + (end - start) * percent);
	}

	public float getRotation () {
		return end;
	}

	public void setRotation (float rotation) {
		this.end = rotation;
	}

	public boolean isUseShortestDirection () {
		return useShortestDirection;
	}

	public void setUseShortestDirection (boolean useShortestDirection) {
		this.useShortestDirection = useShortestDirection;
	}
}
