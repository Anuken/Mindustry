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

import com.badlogic.gdx.utils.Pool;

import io.anuke.ucore.scene.Action;
import io.anuke.ucore.scene.Element;

/** Base class for an action that wraps another action.
 * @author Nathan Sweet */
abstract public class DelegateAction extends Action {
	protected Action action;

	/** Sets the wrapped action. */
	public void setAction (Action action) {
		this.action = action;
	}

	public Action getAction () {
		return action;
	}

	abstract protected boolean delegate (float delta);

	public final boolean act (float delta) {
		Pool pool = getPool();
		setPool(null); // Ensure this action can't be returned to the pool inside the delegate action.
		try {
			return delegate(delta);
		} finally {
			setPool(pool);
		}
	}

	public void restart () {
		if (action != null) action.restart();
	}

	public void reset () {
		super.reset();
		action = null;
	}

	public void setActor (Element actor) {
		if (action != null) action.setActor(actor);
		super.setActor(actor);
	}

	public void setTarget (Element target) {
		if (action != null) action.setTarget(target);
		super.setTarget(target);
	}

	public String toString () {
		return super.toString() + (action == null ? "" : "(" + action + ")");
	}
}
