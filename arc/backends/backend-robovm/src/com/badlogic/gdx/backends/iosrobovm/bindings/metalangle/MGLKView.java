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

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIView;
import org.robovm.objc.ObjCRuntime;
import org.robovm.objc.annotation.Method;
import org.robovm.objc.annotation.NativeClass;
import org.robovm.objc.annotation.Property;
import org.robovm.rt.bro.annotation.ByVal;
import org.robovm.rt.bro.annotation.MachineSizedSInt;
import org.robovm.rt.bro.annotation.Pointer;
import org.robovm.rt.bro.ptr.Ptr;
/*</imports>*/

/*<javadoc>*/

/*</javadoc>*/
/*<annotations>*/@NativeClass
/* </annotations> */
/* <visibility> */public/* </visibility> */ class /* <name> */ MGLKView/* </name> */
	extends /* <extends> */UIView/* </extends> */
/* <implements> *//* </implements> */ {

	/* <ptr> */public static class MGLKViewPtr extends Ptr<MGLKView, MGLKViewPtr> {
	}

	/* </ptr> */
	/* <bind> */static {
		ObjCRuntime.bind(MGLKView.class);
	}/* </bind> */
	/* <constants> *//* </constants> */
	/* <constructors> */

	protected MGLKView () {
	}

	protected MGLKView (Handle h, long handle) {
		super(h, handle);
	}

	protected MGLKView (SkipInit skipInit) {
		super(skipInit);
	}

	@Method(selector = "initWithFrame:context:")
	public MGLKView (@ByVal CGRect frame, MGLContext context) {
		super((SkipInit)null);
		initObject(initWithFrameContext(frame, context));
	}

	/* </constructors> */
	/* <properties> */
	@Property(selector = "context")
	public native MGLContext getContext ();

	@Property(selector = "setContext:")
	public native void setContext (MGLContext v);

	@Property(selector = "delegate")
	public native MGLKViewDelegate getDelegate ();

	@Property(selector = "setDelegate:", strongRef = true)
	public native void setDelegate (MGLKViewDelegate v);

	@Property(selector = "retainedBacking")
	public native boolean isRetainedBacking ();

	@Property(selector = "setRetainedBacking:")
	public native void setRetainedBacking (boolean v);

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

	@Property(selector = "glLayer")
	public native MGLLayer getGlLayer ();

	@Property(selector = "drawableSize")
	public native @ByVal CGSize getDrawableSize ();

	@Property(selector = "drawableWidth")
	public native @MachineSizedSInt long getDrawableWidth ();

	@Property(selector = "drawableHeight")
	public native @MachineSizedSInt long getDrawableHeight ();

	@Property(selector = "defaultOpenGLFrameBufferID")
	public native int getDefaultOpenGLFrameBufferID ();

	@Property(selector = "enableSetNeedsDisplay")
	public native boolean isEnableSetNeedsDisplay ();

	@Property(selector = "setEnableSetNeedsDisplay:")
	public native void setEnableSetNeedsDisplay (boolean v);

	@Property(selector = "snapshot")
	public native UIImage getSnapshot ();

	/* </properties> */
	/* <members> *//* </members> */
	/* <methods> */
	@Method(selector = "initWithFrame:context:")
	protected native @Pointer long initWithFrameContext (@ByVal CGRect frame, MGLContext context);

	@Method(selector = "display")
	public native void display ();

	@Method(selector = "bindDrawable")
	public native void bindDrawable ();
	/* </methods> */
}
