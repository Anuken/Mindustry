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
import org.robovm.objc.annotation.Method;
import org.robovm.objc.annotation.NativeClass;
import org.robovm.objc.annotation.Property;
import org.robovm.rt.bro.annotation.Pointer;
import org.robovm.rt.bro.ptr.Ptr;
import org.robovm.rt.bro.ptr.VoidPtr;
/*</imports>*/

/*<javadoc>*/

/*</javadoc>*/
/*<annotations>*/@NativeClass
/* </annotations> */
/* <visibility> */public/* </visibility> */ class /* <name> */ MGLContext/* </name> */
	extends /* <extends> */NSObject/* </extends> */
/* <implements> *//* </implements> */ {

	/* <ptr> */public static class MGLContextPtr extends Ptr<MGLContext, MGLContextPtr> {
	}

	/* </ptr> */
	/* <bind> */static {
		ObjCRuntime.bind(MGLContext.class);
	}/* </bind> */
	/* <constants> *//* </constants> */
	/* <constructors> */

	protected MGLContext () {
	}

	protected MGLContext (Handle h, long handle) {
		super(h, handle);
	}

	protected MGLContext (SkipInit skipInit) {
		super(skipInit);
	}

	@Method(selector = "initWithAPI:")
	public MGLContext (MGLRenderingAPI api) {
		super((SkipInit)null);
		initObject(initWithAPI(api));
	}

	@Method(selector = "initWithAPI:sharegroup:")
	public MGLContext (MGLRenderingAPI api, MGLSharegroup sharegroup) {
		super((SkipInit)null);
		initObject(initWithAPISharegroup(api, sharegroup));
	}

	/* </constructors> */
	/* <properties> */
	@Property(selector = "API")
	public native MGLRenderingAPI getAPI ();

	@Property(selector = "sharegroup")
	public native MGLSharegroup getSharegroup ();

	@Property(selector = "eglDisplay")
	public native VoidPtr getEglDisplay ();

	/* </properties> */
	/* <members> *//* </members> */
	/* <methods> */
	@Method(selector = "initWithAPI:")
	protected native @Pointer long initWithAPI (MGLRenderingAPI api);

	@Method(selector = "initWithAPI:sharegroup:")
	protected native @Pointer long initWithAPISharegroup (MGLRenderingAPI api, MGLSharegroup sharegroup);

	@Method(selector = "present:")
	public native boolean present (MGLLayer layer);

	@Method(selector = "currentContext")
	public static native MGLContext currentContext ();

	@Method(selector = "currentLayer")
	public static native MGLLayer currentLayer ();

	@Method(selector = "setCurrentContext:")
	public static native boolean setCurrentContext (MGLContext context);

	@Method(selector = "setCurrentContext:forLayer:")
	public static native boolean setCurrentContextForLayer (MGLContext context, MGLLayer layer);
	/* </methods> */
}
