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
import io.anuke.ucore.scene.utils.Layout;

import com.badlogic.gdx.utils.GdxRuntimeException;

/** Sets an actor's {@link Layout#setLayoutEnabled(boolean) layout} to enabled or disabled. The actor must implements
 * {@link Layout}.
 * @author Nathan Sweet */
public class LayoutAction extends Action {
	private boolean enabled;

	public void setTarget (Element actor) {
		if (actor != null && !(actor instanceof Layout)) throw new GdxRuntimeException("Actor must implement layout: " + actor);
		super.setTarget(actor);
	}

	public boolean act (float delta) {
		((Layout)target).setLayoutEnabled(enabled);
		return true;
	}

	public boolean isEnabled () {
		return enabled;
	}

	public void setLayoutEnabled (boolean enabled) {
		this.enabled = enabled;
	}
}
