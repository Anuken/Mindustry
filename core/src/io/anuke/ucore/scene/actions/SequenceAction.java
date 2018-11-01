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

/** Executes a number of actions one at a time.
 * @author Nathan Sweet */
public class SequenceAction extends ParallelAction {
	private int index;

	public SequenceAction () {
	}

	public SequenceAction (Action action1) {
		addAction(action1);
	}

	public SequenceAction (Action action1, Action action2) {
		addAction(action1);
		addAction(action2);
	}

	public SequenceAction (Action action1, Action action2, Action action3) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
	}

	public SequenceAction (Action action1, Action action2, Action action3, Action action4) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
		addAction(action4);
	}

	public SequenceAction (Action action1, Action action2, Action action3, Action action4, Action action5) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
		addAction(action4);
		addAction(action5);
	}

	public boolean act (float delta) {
		if (index >= actions.size) return true;
		Pool pool = getPool();
		setPool(null); // Ensure this action can't be returned to the pool while executings.
		try {
			if (actions.get(index).act(delta)) {
				if (actor == null) return true; // This action was removed.
				index++;
				if (index >= actions.size) return true;
			}
			return false;
		} finally {
			setPool(pool);
		}
	}

	public void restart () {
		super.restart();
		index = 0;
	}
}
