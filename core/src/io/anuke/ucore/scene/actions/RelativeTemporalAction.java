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

/** Base class for actions that transition over time using the percent complete since the last frame.
 * @author Nathan Sweet */
abstract public class RelativeTemporalAction extends TemporalAction {
	private float lastPercent;

	protected void begin () {
		lastPercent = 0;
	}

	protected void update (float percent) {
		updateRelative(percent - lastPercent);
		lastPercent = percent;
	}

	abstract protected void updateRelative (float percentDelta);
}
