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

import org.robovm.apple.foundation.NSObjectProtocol;
import org.robovm.objc.annotation.Method;
/*</imports>*/

/*<javadoc>*/

/*</javadoc>*/
/*<annotations>*//*</annotations>*/
/*<visibility>*/public/* </visibility> */ interface /* <name> */ MGLKViewControllerDelegate/* </name> */
	/* <implements> */ extends NSObjectProtocol/* </implements> */ {

	/* <ptr> */
	/* </ptr> */
	/* <bind> */
	/* </bind> */
	/* <constants> *//* </constants> */
	/* <properties> */

	/* </properties> */
	/* <methods> */
	@Method(selector = "mglkViewControllerUpdate:")
	void update (MGLKViewController controller);
	/* </methods> */
	/* <adapter> */
	/* </adapter> */
}
