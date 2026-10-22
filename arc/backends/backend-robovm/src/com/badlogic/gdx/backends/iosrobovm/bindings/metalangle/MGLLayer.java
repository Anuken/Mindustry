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

import org.robovm.apple.coreanimation.CALayer;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.objc.ObjCRuntime;
import org.robovm.objc.annotation.Method;
import org.robovm.objc.annotation.NativeClass;
import org.robovm.objc.annotation.Property;
import org.robovm.rt.bro.annotation.ByVal;
import org.robovm.rt.bro.ptr.Ptr;
/*</imports>*/

/*<javadoc>*/

/*</javadoc>*/
/*<annotations>*/@NativeClass
/* </annotations> */
/* <visibility> */public/* </visibility> */ class /* <name> */ MGLLayer/* </name> */
	extends /* <extends> */CALayer/* </extends> */
/* <implements> *//* </implements> */ {

	/* <ptr> */public static class MGLLayerPtr extends Ptr<MGLLayer, MGLLayerPtr> {
	}

	/* </ptr> */
	/* <bind> */static {
		ObjCRuntime.bind(MGLLayer.class);
	}/* </bind> */
	/* <constants> *//* </constants> */
	/* <constructors> */

	protected MGLLayer () {
	}

	protected MGLLayer (Handle h, long handle) {
		super(h, handle);
	}

	protected MGLLayer (SkipInit skipInit) {
		super(skipInit);
	}

	/* </constructors> */
	/* <properties> */
	@Property(selector = "drawableSize")
	public native @ByVal CGSize getDrawableSize ();

	@Property(selector = "defaultOpenGLFrameBufferID")
	public native int getDefaultOpenGLFrameBufferID ();

	@Property(selector = "drawableColorFormat")
	public native MGLDrawableColorFormat getDrawableColorFormat ();

	@Property(selector = "setDrawableColorFormat:")
	public native void setDrawableColorFormat (MGLDrawableColorFormat v);

	@Property(selector = "drawableDepthFormat")
	public native MGLDrawableDepthFormat getDrawableDepthFormat ();

	@Property(selector = "setDrawableDepthFormat:")
	public native void setDrawableDepthFormat (MGLDrawableDepthFormat v);

	@Property(selector = "drawableStencilFormat")
	public native MGLDrawableStencilFormat getDrawableStencilFormat ();

	@Property(selector = "setDrawableStencilFormat:")
	public native void setDrawableStencilFormat (MGLDrawableStencilFormat v);

	@Property(selector = "drawableMultisample")
	public native MGLDrawableMultisample getDrawableMultisample ();

	@Property(selector = "setDrawableMultisample:")
	public native void setDrawableMultisample (MGLDrawableMultisample v);

	@Property(selector = "retainedBacking")
	public native boolean isRetainedBacking ();

	@Property(selector = "setRetainedBacking:")
	public native void setRetainedBacking (boolean v);

	/* </properties> */
	/* <members> *//* </members> */
	/* <methods> */
	@Method(selector = "present")
	public native boolean present ();

	@Method(selector = "bindDefaultFrameBuffer")
	public native void bindDefaultFrameBuffer ();
	/* </methods> */
}
