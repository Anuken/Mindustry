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

package io.anuke.ucore.scene.event;

import com.badlogic.gdx.math.Interpolation;

import io.anuke.ucore.scene.ui.ScrollPane;

import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

/** Causes a scroll pane to scroll when a drag goes outside the bounds of the scroll pane.
 * @author Nathan Sweet */
public class DragScrollListener extends DragListener {
	private ScrollPane scroll;
	private Task scrollUp, scrollDown;
	Interpolation interpolation = Interpolation.exp5In;
	float minSpeed = 15, maxSpeed = 75, tickSecs = 0.05f;
	long startTime, rampTime = 1750;

	public DragScrollListener (final ScrollPane scroll) {
		this.scroll = scroll;

		scrollUp = new Task() {
			public void run () {
				scroll.setScrollY(scroll.getScrollY() - getScrollPixels());
			}
		};
		scrollDown = new Task() {
			public void run () {
				scroll.setScrollY(scroll.getScrollY() + getScrollPixels());
			}
		};
	}

	public void setup (float minSpeedPixels, float maxSpeedPixels, float tickSecs, float rampSecs) {
		this.minSpeed = minSpeedPixels;
		this.maxSpeed = maxSpeedPixels;
		this.tickSecs = tickSecs;
		rampTime = (long)(rampSecs * 1000);
	}

	float getScrollPixels () {
		return interpolation.apply(minSpeed, maxSpeed, Math.min(1, (System.currentTimeMillis() - startTime) / (float)rampTime));
	}

	public void drag (InputEvent event, float x, float y, int pointer) {
		if (x >= 0 && x < scroll.getWidth()) {
			if (y >= scroll.getHeight()) {
				scrollDown.cancel();
				if (!scrollUp.isScheduled()) {
					startTime = System.currentTimeMillis();
					Timer.schedule(scrollUp, tickSecs, tickSecs);
				}
				return;
			} else if (y < 0) {
				scrollUp.cancel();
				if (!scrollDown.isScheduled()) {
					startTime = System.currentTimeMillis();
					Timer.schedule(scrollDown, tickSecs, tickSecs);
				}
				return;
			}
		}
		scrollUp.cancel();
		scrollDown.cancel();
	}

	public void dragStop (InputEvent event, float x, float y, int pointer) {
		scrollUp.cancel();
		scrollDown.cancel();
	}
}
