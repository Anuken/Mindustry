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

/** Sets the actor's rotation from its current value to a relative value.
 * @author Nathan Sweet */
public class RotateByAction extends RelativeTemporalAction {
	private float amount;

	protected void updateRelative (float percentDelta) {
		target.rotateBy(amount * percentDelta);
	}

	public float getAmount () {
		return amount;
	}

	public void setAmount (float rotationAmount) {
		amount = rotationAmount;
	}
}
