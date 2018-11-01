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

package io.anuke.ucore.scene.utils;

import com.badlogic.gdx.graphics.g2d.Batch;

import io.anuke.ucore.scene.Element;

/** Provides methods for an actor to participate in layout and to provide a minimum, preferred, and maximum size.
 * @author Nathan Sweet */
public interface Layout {
	/** Computes and caches any information needed for drawing and, if this actor has children, positions and sizes each child,
	 * calls {@link #invalidate()} on any each child whose width or height has changed, and calls {@link #validate()} on each
	 * child. This method should almost never be called directly, instead {@link #validate()} should be used. */
	public void layout ();

	/** Invalidates this actor's layout, causing {@link #layout()} to happen the next time {@link #validate()} is called. This
	 * method should be called when state changes in the actor that requires a layout but does not change the minimum, preferred,
	 * maximum, or actual size of the actor (meaning it does not affect the parent actor's layout). */
	public void invalidate ();

	/** Invalidates this actor and all its ancestors, calling {@link #invalidate()} on each. This method should be called when
	 * state changes in the actor that affects the minimum, preferred, maximum, or actual size of the actor (meaning it potentially
	 * affects the parent actor's layout). */
	public void invalidateHierarchy ();

	/** Ensures the actor has been laid out. Calls {@link #layout()} if {@link #invalidate()} has been called since the last time
	 * {@link #validate()} was called, or if the actor otherwise needs to be laid out. This method is usually called in
	 * {@link Element#draw(Batch, float)} by the actor itself before drawing is performed. */
	public void validate ();

	/** Sizes this actor to its preferred width and height, then calls {@link #validate()}.
	 * <p>
	 * Generally this method should not be called in an actor's constructor because it calls {@link #layout()}, which means a
	 * subclass would have layout() called before the subclass' constructor. Instead, in constructors simply set the actor's size
	 * to {@link #getPrefWidth()} and {@link #getPrefHeight()}. This allows the actor to have a size at construction time for more
	 * convenient use with groups that do not layout their children. */
	public void pack ();

	/** If true, this actor will be sized to the parent in {@link #validate()}. If the parent is the stage, the actor will be sized
	 * to the stage. This method is for convenience only when the widget's parent does not set the size of its children (such as
	 * the stage). */
	public void setFillParent (boolean fillParent);

	/** Enables or disables the layout for this actor and all child actors, recursively. When false, {@link #validate()} will not
	 * cause a layout to occur. This can be useful when an actor will be manipulated externally, such as with actions. Default is
	 * true. */
	public void setLayoutEnabled (boolean enabled);

	public float getMinWidth ();

	public float getMinHeight ();

	public float getPrefWidth ();

	public float getPrefHeight ();

	/** Zero indicates no max width. */
	public float getMaxWidth ();

	/** Zero indicates no max height. */
	public float getMaxHeight ();
}
