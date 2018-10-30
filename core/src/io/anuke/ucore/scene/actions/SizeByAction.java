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

/** Moves an actor from its current size to a relative size.
 * @author Nathan Sweet */
public class SizeByAction extends RelativeTemporalAction {
	private float amountWidth, amountHeight;

	protected void updateRelative (float percentDelta) {
		target.sizeBy(amountWidth * percentDelta, amountHeight * percentDelta);
	}

	public void setAmount (float width, float height) {
		amountWidth = width;
		amountHeight = height;
	}

	public float getAmountWidth () {
		return amountWidth;
	}

	public void setAmountWidth (float width) {
		amountWidth = width;
	}

	public float getAmountHeight () {
		return amountHeight;
	}

	public void setAmountHeight (float height) {
		amountHeight = height;
	}
}
