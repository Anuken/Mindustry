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

/** Moves an actor to a relative position.
 * @author Nathan Sweet */
public class TranslateByAction extends RelativeTemporalAction {
	private float amountX, amountY;

	protected void updateRelative (float percentDelta) {
		target.getTranslation().add(amountX * percentDelta, amountY * percentDelta);
	}

	public void setAmount (float x, float y) {
		amountX = x;
		amountY = y;
	}

	public float getAmountX () {
		return amountX;
	}

	public void setAmountX (float x) {
		amountX = x;
	}

	public float getAmountY () {
		return amountY;
	}

	public void setAmountY (float y) {
		amountY = y;
	}
}
