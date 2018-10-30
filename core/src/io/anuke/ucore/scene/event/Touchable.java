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

/** Determines how touch input events are distributed to an actor and any children.
 * @author Nathan Sweet */
public enum Touchable {
	/** All touch input events will be received by the actor and any children. */
	enabled,
	/** No touch input events will be received by the actor or any children. */
	disabled,
	/** No touch input events will be received by the actor, but children will still receive events. Note that events on the
	 * children will still bubble to the parent. */
	childrenOnly
}
