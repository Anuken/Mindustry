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

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import io.anuke.ucore.scene.Action;
import io.anuke.ucore.scene.Element;

/** Executes a number of actions at the same time.
 * @author Nathan Sweet */
public class ParallelAction extends Action {
	Array<Action> actions = new Array(4);
	private boolean complete;

	public ParallelAction () {
	}

	public ParallelAction (Action action1) {
		addAction(action1);
	}

	public ParallelAction (Action action1, Action action2) {
		addAction(action1);
		addAction(action2);
	}

	public ParallelAction (Action action1, Action action2, Action action3) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
	}

	public ParallelAction (Action action1, Action action2, Action action3, Action action4) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
		addAction(action4);
	}

	public ParallelAction (Action action1, Action action2, Action action3, Action action4, Action action5) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
		addAction(action4);
		addAction(action5);
	}

	public boolean act (float delta) {
		if (complete) return true;
		complete = true;
		Pool pool = getPool();
		setPool(null); // Ensure this action can't be returned to the pool while executing.
		try {
			Array<Action> actions = this.actions;
			for (int i = 0, n = actions.size; i < n && actor != null; i++) {
				Action currentAction = actions.get(i);
				if (currentAction.getActor() != null && !currentAction.act(delta)) complete = false;
				if (actor == null) return true; // This action was removed.
			}
			return complete;
		} finally {
			setPool(pool);
		}
	}

	public void restart () {
		complete = false;
		Array<Action> actions = this.actions;
		for (int i = 0, n = actions.size; i < n; i++)
			actions.get(i).restart();
	}

	public void reset () {
		super.reset();
		actions.clear();
	}

	public void addAction (Action action) {
		actions.add(action);
		if (actor != null) action.setActor(actor);
	}

	public void setActor (Element actor) {
		Array<Action> actions = this.actions;
		for (int i = 0, n = actions.size; i < n; i++)
			actions.get(i).setActor(actor);
		super.setActor(actor);
	}

	public Array<Action> getActions () {
		return actions;
	}

	public String toString () {
		StringBuilder buffer = new StringBuilder(64);
		buffer.append(super.toString());
		buffer.append('(');
		Array<Action> actions = this.actions;
		for (int i = 0, n = actions.size; i < n; i++) {
			if (i > 0) buffer.append(", ");
			buffer.append(actions.get(i));
		}
		buffer.append(')');
		return buffer.toString();
	}
}
