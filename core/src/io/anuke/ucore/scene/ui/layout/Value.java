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

package io.anuke.ucore.scene.ui.layout;

import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.scene.Element;

/** Value placeholder, allowing the value to be computed on request. Values are provided an actor for context which reduces the
 * number of value instances that need to be created and reduces verbosity in code that specifies values.
 * @author Nathan Sweet */
abstract public class Value {
	static int amount = 0;
	
	/** @param context May be null. */
	abstract public float get (Element context);

	/** A value that is always zero. */
	static public final Fixed zero = new Fixed(0);
	
	public static Value create(Supplier<Float> prov){
		return new Value(){
			@Override
			public float get(Element context){
				return prov.get();
			}
		};
	}

	/** A fixed value that is not computed each time it is used.
	 * @author Nathan Sweet */
	static public class Fixed extends Value {
		private final float value;

		public Fixed (float value) {
			this.value = value;
		}

		public float get (Element context) {
			return Unit.dp.scl(value);
		}
	}

	/** Value that is the minWidth of the actor in the cell. */
	static public Value minWidth = new Value() {
		public float get (Element context) {
			return context == null ? 0 : context.getMinWidth();
		}
	};

	/** Value that is the minHeight of the actor in the cell. */
	static public Value minHeight = new Value() {
		public float get (Element context) {
			return context == null ? 0 : context.getMinHeight();
		}
	};

	/** Value that is the prefWidth of the actor in the cell. */
	static public Value prefWidth = new Value() {
		public float get (Element context) {
			return context == null ? 0 : context.getPrefWidth();

		}
	};

	/** Value that is the prefHeight of the actor in the cell. */
	static public Value prefHeight = new Value() {
		public float get (Element context) {
			return context == null ? 0 : context.getPrefHeight();
		}
	};

	/** Value that is the maxWidth of the actor in the cell. */
	static public Value maxWidth = new Value() {
		public float get (Element context) {
			return context == null ? 0 : context.getMaxWidth();
		}
	};

	/** Value that is the maxHeight of the actor in the cell. */
	static public Value maxHeight = new Value() {
		public float get (Element context) {
			return context == null ? 0 : context.getMaxHeight();
		}
	};

	/** Returns a value that is a percentage of the actor's width. */
	static public Value percentWidth (final float percent) {
		return new Value() {
			public float get (Element actor) {
				return actor.getWidth() * percent;
			}
		};
	}

	/** Returns a value that is a percentage of the actor's height. */
	static public Value percentHeight (final float percent) {
		return new Value() {
			public float get (Element actor) {
				return actor.getHeight() * percent;
			}
		};
	}

	/** Returns a value that is a percentage of the specified actor's width. The context actor is ignored. */
	static public Value percentWidth (final float percent, final Element actor) {
		if (actor == null) throw new IllegalArgumentException("actor cannot be null.");
		return new Value() {
			public float get (Element context) {
				return actor.getWidth() * percent;
			}
		};
	}

	/** Returns a value that is a percentage of the specified actor's height. The context actor is ignored. */
	static public Value percentHeight (final float percent, final Element actor) {
		if (actor == null) throw new IllegalArgumentException("actor cannot be null.");
		return new Value() {
			public float get (Element context) {
				return actor.getHeight() * percent;
			}
		};
	}
}
