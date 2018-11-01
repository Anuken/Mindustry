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

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.scene.Element;

/** Sets the actor's color (or a specified color), from the current to the new color. Note this action transitions from the color
 * at the time the action starts to the specified color.
 * @author Nathan Sweet */
public class ColorAction extends TemporalAction {
	private float startR, startG, startB, startA;
	private Color color;
	private final Color end = new Color();

	protected void begin () {
		if (color == null) color = target.getColor();
		startR = color.r;
		startG = color.g;
		startB = color.b;
		startA = color.a;
	}

	protected void update (float percent) {
		float r = startR + (end.r - startR) * percent;
		float g = startG + (end.g - startG) * percent;
		float b = startB + (end.b - startB) * percent;
		float a = startA + (end.a - startA) * percent;
		color.set(r, g, b, a);
	}

	public void reset () {
		super.reset();
		color = null;
	}

	public Color getColor () {
		return color;
	}

	/** Sets the color to modify. If null (the default), the {@link #getActor() actor's} {@link Element#getColor() color} will be
	 * used. */
	public void setColor (Color color) {
		this.color = color;
	}

	public Color getEndColor () {
		return end;
	}

	/** Sets the color to transition to. Required. */
	public void setEndColor (Color color) {
		end.set(color);
	}
}
