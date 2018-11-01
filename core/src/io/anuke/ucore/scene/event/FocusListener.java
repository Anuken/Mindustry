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

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.Event;
import io.anuke.ucore.scene.event.EventListener;

/** Listener for {@link FocusEvent}.
 * @author Nathan Sweet */
abstract public class FocusListener implements EventListener {
	public boolean handle (Event event) {
		if (!(event instanceof FocusEvent)) return false;
		FocusEvent focusEvent = (FocusEvent)event;
		switch (focusEvent.getType()) {
		case keyboard:
			keyboardFocusChanged(focusEvent, event.getTarget(), focusEvent.isFocused());
			break;
		case scroll:
			scrollFocusChanged(focusEvent, event.getTarget(), focusEvent.isFocused());
			break;
		}
		return false;
	}

	/** @param actor The event target, which is the actor that emitted the focus event. */
	public void keyboardFocusChanged (FocusEvent event, Element actor, boolean focused) {
	}

	/** @param actor The event target, which is the actor that emitted the focus event. */
	public void scrollFocusChanged (FocusEvent event, Element actor, boolean focused) {
	}

	/** Fired when an actor gains or loses keyboard or scroll focus. Can be cancelled to prevent losing or gaining focus.
	 * @author Nathan Sweet */
	static public class FocusEvent extends Event {
		private boolean focused;
		private Type type;
		private Element relatedActor;

		public void reset () {
			super.reset();
			relatedActor = null;
		}

		public boolean isFocused () {
			return focused;
		}

		public void setFocused (boolean focused) {
			this.focused = focused;
		}

		public Type getType () {
			return type;
		}

		public void setType (Type focusType) {
			this.type = focusType;
		}

		/** The actor related to the event. When focus is lost, this is the new actor being focused, or null. When focus is gained,
		 * this is the previous actor that was focused, or null. */
		public Element getRelatedActor () {
			return relatedActor;
		}

		/** @param relatedActor May be null. */
		public void setRelatedActor (Element relatedActor) {
			this.relatedActor = relatedActor;
		}

		/** @author Nathan Sweet */
		static public enum Type {
			keyboard, scroll
		}
	}
}
