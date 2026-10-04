/*
 * Copyright (C) 2013-2015 RoboVM AB
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.badlogic.gdx.backends.iosrobovm.bindings.metalangle;

/*<imports>*/

import org.robovm.apple.foundation.NSObject;
import org.robovm.objc.ObjCRuntime;
import org.robovm.objc.annotation.NativeClass;
import org.robovm.rt.bro.ptr.Ptr;
/*</imports>*/

/*<javadoc>*/

/*</javadoc>*/
/*<annotations>*/@NativeClass
/* </annotations> */
/* <visibility> */public/* </visibility> */ class /* <name> */ MGLSharegroup/* </name> */
	extends /* <extends> */NSObject/* </extends> */
/* <implements> *//* </implements> */ {

	/* <ptr> */public static class MGLSharegroupPtr extends Ptr<MGLSharegroup, MGLSharegroupPtr> {
	}

	/* </ptr> */
	/* <bind> */static {
		ObjCRuntime.bind(MGLSharegroup.class);
	}/* </bind> */
	/* <constants> *//* </constants> */
	/* <constructors> */

	protected MGLSharegroup () {
	}

	protected MGLSharegroup (Handle h, long handle) {
		super(h, handle);
	}

	protected MGLSharegroup (SkipInit skipInit) {
		super(skipInit);
	}
	/* </constructors> */
	/* <properties> */

	/* </properties> */
	/* <members> *//* </members> */
	/* <methods> */

	/* </methods> */
}
