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

/** Low level interface for receiving events. Typically there is a listener class for each specific event class.
 * @see InputListener
 * @see InputEvent
 * @author Nathan Sweet */
public interface EventListener {
	/** Try to handle the given event, if it is applicable.
	 * @return true if the event should be considered {@link Event#handle() handled} by scene2d. */
	public boolean handle (Event event);
}
