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

import io.anuke.ucore.scene.Action;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.Touchable;

/** Sets the actor's {@link Element#setTouchable(Touchable) touchability}.
 * @author Nathan Sweet */
public class TouchableAction extends Action {
	private Touchable touchable;

	public boolean act (float delta) {
		target.setTouchable(touchable);
		return true;
	}

	public Touchable getTouchable () {
		return touchable;
	}

	public void setTouchable (Touchable touchable) {
		this.touchable = touchable;
	}
}
