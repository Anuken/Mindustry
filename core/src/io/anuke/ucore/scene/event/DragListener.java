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

import com.badlogic.gdx.Input.Buttons;

import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;

/** Detects mouse or finger touch drags on an actor. A touch must go down over the actor and a drag won't start until it is moved
 * outside the {@link #setTapSquareSize(float) tap square}. Any touch (not just the first) will trigger this listener. While
 * pressed, other touch downs are ignored.
 * @author Nathan Sweet */
public class DragListener extends InputListener {
	private float tapSquareSize = 14, touchDownX = -1, touchDownY = -1, stageTouchDownX = -1, stageTouchDownY = -1;
	private int pressedPointer = -1;
	private int button;
	private boolean dragging;
	private float deltaX, deltaY;

	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
		if (pressedPointer != -1) return false;
		if (pointer == 0 && this.button != -1 && button != this.button) return false;
		pressedPointer = pointer;
		touchDownX = x;
		touchDownY = y;
		stageTouchDownX = event.getStageX();
		stageTouchDownY = event.getStageY();
		return true;
	}

	public void touchDragged (InputEvent event, float x, float y, int pointer) {
		if (pointer != pressedPointer) return;
		if (!dragging && (Math.abs(touchDownX - x) > tapSquareSize || Math.abs(touchDownY - y) > tapSquareSize)) {
			dragging = true;
			dragStart(event, x, y, pointer);
			deltaX = x;
			deltaY = y;
		}
		if (dragging) {
			deltaX -= x;
			deltaY -= y;
			drag(event, x, y, pointer);
			deltaX = x;
			deltaY = y;
		}
	}

	public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
		if (pointer == pressedPointer) {
			if (dragging) dragStop(event, x, y, pointer);
			cancel();
		}
	}

	public void dragStart (InputEvent event, float x, float y, int pointer) {
	}

	public void drag (InputEvent event, float x, float y, int pointer) {
	}

	public void dragStop (InputEvent event, float x, float y, int pointer) {
	}

	/* If a drag is in progress, no further drag methods will be called until a new drag is started. */
	public void cancel () {
		dragging = false;
		pressedPointer = -1;
	}

	/** Returns true if a touch has been dragged outside the tap square. */
	public boolean isDragging () {
		return dragging;
	}

	public void setTapSquareSize (float halfTapSquareSize) {
		tapSquareSize = halfTapSquareSize;
	}

	public float getTapSquareSize () {
		return tapSquareSize;
	}

	public float getTouchDownX () {
		return touchDownX;
	}

	public float getTouchDownY () {
		return touchDownY;
	}

	public float getStageTouchDownX () {
		return stageTouchDownX;
	}

	public float getStageTouchDownY () {
		return stageTouchDownY;
	}

	/** Returns the amount on the x axis that the touch has been dragged since the last drag event. */
	public float getDeltaX () {
		return deltaX;
	}

	/** Returns the amount on the y axis that the touch has been dragged since the last drag event. */
	public float getDeltaY () {
		return deltaY;
	}

	public int getButton () {
		return button;
	}

	/** Sets the button to listen for, all other buttons are ignored. Default is {@link Buttons#LEFT}. Use -1 for any button. */
	public void setButton (int button) {
		this.button = button;
	}
}
