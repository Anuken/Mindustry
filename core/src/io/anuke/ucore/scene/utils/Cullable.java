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

import com.badlogic.gdx.math.Rectangle;

import io.anuke.ucore.scene.Group;

/** Allows a parent to set the area that is visible on a child actor to allow the child to cull when drawing itself. This must only
 * be used for actors that are not rotated or scaled.
 * <p>
 * When Group is given a culling rectangle with {@link Group#setCullingArea(Rectangle)}, it will automatically call
 * {@link #setCullingArea(Rectangle)} on its children.
 * @author Nathan Sweet */
public interface Cullable {
	/** @param cullingArea The culling area in the child actor's coordinates. */
	public void setCullingArea (Rectangle cullingArea);
}
