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

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.SnapshotArray;

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.utils.Layout;

/** A {@link Group} that participates in layout and provides a minimum, preferred, and maximum size.
 * <p>
 * The default preferred size of a widget group is 0 and this is almost always overridden by a subclass. The default minimum size
 * returns the preferred size, so a subclass may choose to return 0 for minimum size if it wants to allow itself to be sized
 * smaller than the preferred size. The default maximum size is 0, which means no maximum size.
 * <p>
 * See {@link Layout} for details on how a widget group should participate in layout. A widget group's mutator methods should call
 * {@link #invalidate()} or {@link #invalidateHierarchy()} as needed. By default, invalidateHierarchy is called when child widgets
 * are added and removed.
 * @author Nathan Sweet */
public class WidgetGroup extends Group implements Layout {
	private boolean needsLayout = true;
	private boolean fillParent;
	private boolean layoutEnabled = true;

	public WidgetGroup () {
	}

	/** Creates a new widget group containing the specified actors. */
	public WidgetGroup (Element... actors) {
		for (Element actor : actors)
			addChild(actor);
	}

	public float getMinWidth () {
		return getPrefWidth();
	}

	public float getMinHeight () {
		return getPrefHeight();
	}

	public float getPrefWidth () {
		return 0;
	}

	public float getPrefHeight () {
		return 0;
	}

	public void setLayoutEnabled (boolean enabled) {
		if (layoutEnabled == enabled) return;
		layoutEnabled = enabled;
		setLayoutEnabled(this, enabled);
	}

	private void setLayoutEnabled (Group parent, boolean enabled) {
		SnapshotArray<Element> children = parent.getChildren();
		for (int i = 0, n = children.size; i < n; i++) {
			Element actor = children.get(i);
			if (actor instanceof Layout)
				((Layout)actor).setLayoutEnabled(enabled);
			else if (actor instanceof Group) //
				setLayoutEnabled((Group)actor, enabled);
		}
	}

	public void validate () {
		if (!layoutEnabled) return;

		Group parent = getParent();
		if (fillParent && parent != null) {
			float parentWidth, parentHeight;
			Scene stage = getScene();
			if (stage != null && parent == stage.getRoot()) {
				parentWidth = stage.getWidth();
				parentHeight = stage.getHeight();
			} else {
				parentWidth = parent.getWidth();
				parentHeight = parent.getHeight();
			}
			if (getWidth() != parentWidth || getHeight() != parentHeight) {
				setWidth(parentWidth);
				setHeight(parentHeight);
				invalidate();
			}
		}

		if (!needsLayout) return;
		needsLayout = false;
		layout();
	}

	/** Returns true if the widget's layout has been {@link #invalidate() invalidated}. */
	public boolean needsLayout () {
		return needsLayout;
	}

	public void invalidate () {
		needsLayout = true;
	}

	public void invalidateHierarchy () {
		invalidate();
		Group parent = getParent();
		if (parent instanceof Layout) ((Layout)parent).invalidateHierarchy();
	}

	protected void childrenChanged () {
		invalidateHierarchy();
	}

	protected void sizeChanged () {
		invalidate();
	}

	public void pack () {
		setSize(getPrefWidth(), getPrefHeight());
		validate();
		// Some situations require another layout. Eg, a wrapped label doesn't know its pref height until it knows its width, so it
		// calls invalidateHierarchy() in layout() if its pref height has changed.
		if (needsLayout) {
			setSize(getPrefWidth(), getPrefHeight());
			validate();
		}
	}

	public void setFillParent (boolean fillParent) {
		this.fillParent = fillParent;
	}

	public void layout () {
	}

	/** If this method is overridden, the super method or {@link #validate()} should be called to ensure the widget group is laid
	 * out. */
	public void draw (Batch batch, float parentAlpha) {
		validate();
		super.draw(batch, parentAlpha);
	}
}
