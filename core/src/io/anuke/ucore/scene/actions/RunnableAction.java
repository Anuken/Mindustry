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

/** An action that runs a {@link Runnable}. Alternatively, the {@link #run()} method can be overridden instead of setting a
 * runnable.
 * @author Nathan Sweet */
public class RunnableAction extends Action {
	private Runnable runnable;
	private boolean ran;

	public boolean act (float delta) {
		if (!ran) {
			ran = true;
			run();
		}
		return true;
	}

	/** Called to run the runnable. */
	public void run () {
		Pool pool = getPool();
		setPool(null); // Ensure this action can't be returned to the pool inside the runnable.
		try {
			runnable.run();
		} finally {
			setPool(pool);
		}
	}

	public void restart () {
		ran = false;
	}

	public void reset () {
		super.reset();
		runnable = null;
	}

	public Runnable getRunnable () {
		return runnable;
	}

	public void setRunnable (Runnable runnable) {
		this.runnable = runnable;
	}
}
