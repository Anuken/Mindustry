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

package io.anuke.ucore.scene;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Predicate;
import com.badlogic.gdx.utils.SnapshotArray;

import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.utils.Cullable;

/** 2D scene graph node that may contain other actors.
 * <p>
 * Actors have a z-order equal to the order they were inserted into the group. Actors inserted later will be drawn on top of
 * actors added earlier. Touch events that hit more than one actor are distributed to topmost actors first.
 * @author mzechner
 * @author Nathan Sweet */
public class Group extends Element implements Cullable {
	static private final Vector2 tmp = new Vector2();

	final SnapshotArray<Element> children = new SnapshotArray<>(true, 4, Element.class);
	private final Affine2 worldTransform = new Affine2();
	private final Matrix4 computedTransform = new Matrix4();
	private final Matrix4 oldTransform = new Matrix4();
	boolean transform = true;
	private Rectangle cullingArea;

	public void act (float delta) {
		super.act(delta);
		Element[] actors = children.begin();
		for (int i = 0, n = children.size; i < n; i++)
			actors[i].act(delta);
		children.end();
	}

	/** Draws the group and its children. The default implementation calls {@link #applyTransform(Batch, Matrix4)} if needed, then
	 * {@link #drawChildren(Batch, float)}, then {@link #resetTransform(Batch)} if needed. */
	public void draw (Batch batch, float parentAlpha) {
		if (transform) applyTransform(batch, computeTransform());
		drawChildren(batch, parentAlpha);
		if (transform) resetTransform(batch);
	}

	/** Draws all children. {@link #applyTransform(Batch, Matrix4)} should be called before and {@link #resetTransform(Batch)}
	 * after this method if {@link #setTransform(boolean) transform} is true. If {@link #setTransform(boolean) transform} is false
	 * these methods don't need to be called, children positions are temporarily offset by the group position when drawn. This
	 * method avoids drawing children completely outside the {@link #setCullingArea(Rectangle) culling area}, if set. */
	protected void drawChildren (Batch batch, float parentAlpha) {
		parentAlpha *= this.color.a;
		SnapshotArray<Element> children = this.children;
		Element[] actors = children.begin();
		Rectangle cullingArea = this.cullingArea;
		if (cullingArea != null) {
			// Draw children only if inside culling area.
			float cullLeft = cullingArea.x;
			float cullRight = cullLeft + cullingArea.width;
			float cullBottom = cullingArea.y;
			float cullTop = cullBottom + cullingArea.height;
			if (transform) {
				for (int i = 0, n = children.size; i < n; i++) {
					Element child = actors[i];
					child.alpha = parentAlpha;
					if (!child.isVisible()) continue;
					float cx = child.x, cy = child.y;
					child.x += child.translation.x;
					child.y += child.translation.y;
					if (cx <= cullRight && cy <= cullTop && cx + child.width >= cullLeft && cy + child.height >= cullBottom)
						child.draw(batch, parentAlpha);
					child.x -= child.translation.x;
					child.y -= child.translation.y;
				}
			} else {
				// No transform for this group, offset each child.
				float offsetX = x, offsetY = y;
				x = 0;
				y = 0;
				for (int i = 0, n = children.size; i < n; i++) {
					Element child = actors[i];
					child.alpha = parentAlpha;
					if (!child.isVisible()) continue;
					float cx = child.x, cy = child.y;
					if (cx <= cullRight && cy <= cullTop && cx + child.width >= cullLeft && cy + child.height >= cullBottom) {
						child.x = cx + offsetX + child.getTranslation().x;
						child.y = cy + offsetY + child.getTranslation().y;
						child.draw(batch, parentAlpha);
						child.x = cx;
						child.y = cy;
					}
				}
				x = offsetX;
				y = offsetY;
			}
		} else {
			// No culling, draw all children.
			if (transform) {
				for (int i = 0, n = children.size; i < n; i++) {
					Element child = actors[i];
					child.alpha = parentAlpha;
					if (!child.isVisible()) continue;
					child.x += child.translation.x;
					child.y += child.translation.y;
					child.draw(batch, parentAlpha);
					child.x -= child.translation.x;
					child.y -= child.translation.y;
				}
			} else {
				// No transform for this group, offset each child.
				float offsetX = x, offsetY = y;
				x = 0;
				y = 0;
				for (int i = 0, n = children.size; i < n; i++) {
					Element child = actors[i];
					child.alpha = parentAlpha;
					if (!child.isVisible()) continue;
					float cx = child.x, cy = child.y;
					child.x = cx + offsetX + child.getTranslation().x;
					child.y = cy + offsetY + child.getTranslation().y;
					child.draw(batch, parentAlpha);
					child.x = cx;
					child.y = cy;
				}
				x = offsetX;
				y = offsetY;
			}
		}
		children.end();
	}

	/** Draws this actor's debug lines if {@link #getDebug()} is true and, regardless of {@link #getDebug()}, calls
	 * {@link Element#drawDebug(ShapeRenderer)} on each child. */
	public void drawDebug (ShapeRenderer shapes) {
		drawDebugBounds(shapes);
		if (transform) applyTransform(shapes, computeTransform());
		drawDebugChildren(shapes);
		if (transform) resetTransform(shapes);
	}

	/** Draws all children. {@link #applyTransform(Batch, Matrix4)} should be called before and {@link #resetTransform(Batch)}
	 * after this method if {@link #setTransform(boolean) transform} is true. If {@link #setTransform(boolean) transform} is false
	 * these methods don't need to be called, children positions are temporarily offset by the group position when drawn. This
	 * method avoids drawing children completely outside the {@link #setCullingArea(Rectangle) culling area}, if set. */
	protected void drawDebugChildren (ShapeRenderer shapes) {
		SnapshotArray<Element> children = this.children;
		Element[] actors = children.begin();
		// No culling, draw all children.
		if (transform) {
			for (int i = 0, n = children.size; i < n; i++) {
				Element child = actors[i];
				if (!child.isVisible()) continue;
				if (!child.getDebug() && !(child instanceof Group)) continue;
				child.drawDebug(shapes);
			}
			shapes.flush();
		} else {
			// No transform for this group, offset each child.
			float offsetX = x, offsetY = y;
			x = 0;
			y = 0;
			for (int i = 0, n = children.size; i < n; i++) {
				Element child = actors[i];
				if (!child.isVisible()) continue;
				if (!child.getDebug() && !(child instanceof Group)) continue;
				float cx = child.x, cy = child.y;
				child.x = cx + offsetX;
				child.y = cy + offsetY;
				child.drawDebug(shapes);
				child.x = cx;
				child.y = cy;
			}
			x = offsetX;
			y = offsetY;
		}
		children.end();
	}

	/** Returns the transform for this group's coordinate system. */
	protected Matrix4 computeTransform () {
		Affine2 worldTransform = this.worldTransform;
		float originX = this.originX, originY = this.originY;
		worldTransform.setToTrnRotScl(x + originX, y + originY, rotation, scaleX, scaleY);
		if (originX != 0 || originY != 0) worldTransform.translate(-originX, -originY);

		// Find the first parent that transforms.
		Group parentGroup = parent;
		while (parentGroup != null) {
			if (parentGroup.transform) break;
			parentGroup = parentGroup.parent;
		}
		if (parentGroup != null) worldTransform.preMul(parentGroup.worldTransform);

		computedTransform.set(worldTransform);
		return computedTransform;
	}

	/** Set the batch's transformation matrix, often with the result of {@link #computeTransform()}. Note this causes the batch to
	 * be flushed. {@link #resetTransform(Batch)} will restore the transform to what it was before this call. */
	protected void applyTransform (Batch batch, Matrix4 transform) {
		oldTransform.set(batch.getTransformMatrix());
		batch.setTransformMatrix(transform);
	}

	/** Restores the batch transform to what it was before {@link #applyTransform(Batch, Matrix4)}. Note this causes the batch to
	 * be flushed. */
	protected void resetTransform (Batch batch) {
		batch.setTransformMatrix(oldTransform);
	}

	/** Set the shape renderer transformation matrix, often with the result of {@link #computeTransform()}. Note this causes the
	 * shape renderer to be flushed. {@link #resetTransform(ShapeRenderer)} will restore the transform to what it was before this
	 * call. */
	protected void applyTransform (ShapeRenderer shapes, Matrix4 transform) {
		oldTransform.set(shapes.getTransformMatrix());
		shapes.setTransformMatrix(transform);
	}

	/** Restores the shape renderer transform to what it was before {@link #applyTransform(Batch, Matrix4)}. Note this causes the
	 * shape renderer to be flushed. */
	protected void resetTransform (ShapeRenderer shapes) {
		shapes.setTransformMatrix(oldTransform);
	}

	/** Children completely outside of this rectangle will not be drawn. This is only valid for use with unrotated and unscaled
	 * actors.
	 * @param cullingArea May be null. */
	public void setCullingArea (Rectangle cullingArea) {
		this.cullingArea = cullingArea;
	}

	/** @return May be null.
	 * @see #setCullingArea(Rectangle) */
	public Rectangle getCullingArea () {
		return cullingArea;
	}

	public Element hit (float x, float y, boolean touchable) {
		if (touchable && getTouchable() == Touchable.disabled) return null;
		Vector2 point = tmp;
		Element[] childrenArray = children.items;
		for (int i = children.size - 1; i >= 0; i--) {
			Element child = childrenArray[i];
			if (!child.isVisible()) continue;
			child.parentToLocalCoordinates(point.set(x, y));
			Element hit = child.hit(point.x, point.y, touchable);
			if (hit != null) return hit;
		}
		return super.hit(x, y, touchable);
	}

	/** Called when actors are added to or removed from the group. */
	protected void childrenChanged () {
	}

	public void forEach(Consumer<Element> cons){
		for(Element e : getChildren()){
			cons.accept(e);
			if(e instanceof Group){
				((Group)e).forEach(cons);
			}
		}
	}

	/** Adds an actor as a child of this group, removing it from its previous parent. If the actor is already a child of this
	 * group, no changes are made. */
	public void addChild (Element actor) {
		if (actor.parent != null) {
			if (actor.parent == this) return;
			actor.parent.removeChild(actor, false);
		}
		children.add(actor);
		actor.setParent(this);
		actor.setScene(getScene());
		childrenChanged();
	}

	/** Adds an actor as a child of this group at a specific index, removing it from its previous parent. If the actor is already a
	 * child of this group, no changes are made.
	 * @param index May be greater than the number of children. */
	public void addChildAt (int index, Element actor) {
		if (actor.parent != null) {
			if (actor.parent == this) return;
			actor.parent.removeChild(actor, false);
		}
		if (index >= children.size)
			children.add(actor);
		else
			children.insert(index, actor);
		actor.setParent(this);
		actor.setScene(getScene());
		childrenChanged();
	}

	/** Adds an actor as a child of this group immediately before another child actor, removing it from its previous parent. If the
	 * actor is already a child of this group, no changes are made. */
	public void addChildBefore (Element actorBefore, Element actor) {
		if (actor.parent != null) {
			if (actor.parent == this) return;
			actor.parent.removeChild(actor, false);
		}
		int index = children.indexOf(actorBefore, true);
		children.insert(index, actor);
		actor.setParent(this);
		actor.setScene(getScene());
		childrenChanged();
	}

	/** Adds an actor as a child of this group immediately after another child actor, removing it from its previous parent. If the
	 * actor is already a child of this group, no changes are made. */
	public void addChildAfter (Element actorAfter, Element actor) {
		if (actor.parent != null) {
			if (actor.parent == this) return;
			actor.parent.removeChild(actor, false);
		}
		int index = children.indexOf(actorAfter, true);
		if (index == children.size)
			children.add(actor);
		else
			children.insert(index + 1, actor);
		actor.setParent(this);
		actor.setScene(getScene());
		childrenChanged();
	}

	/** Removes an actor from this group and unfocuses it. Calls {@link #removeChild(Element, boolean)} with true. */
	public boolean removeChild (Element actor) {
		return removeChild(actor, true);
	}

	/** Removes an actor from this group. If the actor will not be used again and has actions, they should be
	 * {@link Element#clearActions() cleared} so the actions will be returned to their
	 * {@link Action#setPool(com.badlogic.gdx.utils.Pool) pool}, if any. This is not done automatically.
	 * @param unfocus If true, {@link Scene#unfocus(Element)} is called.
	 * @return true if the actor was removed from this group. */
	public boolean removeChild (Element actor, boolean unfocus) {
		if (!children.removeValue(actor, true)) return false;
		if (unfocus) {
			Scene stage = getScene();
			if (stage != null) stage.unfocus(actor);
		}
		actor.setParent(null);
		actor.setScene(null);
		childrenChanged();
		return true;
	}

	/** Removes all actors from this group. */
	public void clearChildren () {
		Element[] actors = children.begin();
		for (int i = 0, n = children.size; i < n; i++) {
			Element child = actors[i];
			child.setScene(null);
			child.setParent(null);
		}
		children.end();
		children.clear();
		childrenChanged();
	}

	/** Removes all children, actions, and listeners from this group. */
	public void clear () {
		super.clear();
		clearChildren();
	}

	/** Returns the first actor found with the specified name. Note this recursively compares the name of every actor in the
	 * group. */
	public <T extends Element> T find (String name) {
		Array<Element> children = this.children;
		for (int i = 0, n = children.size; i < n; i++)
			if (name.equals(children.get(i).getName())) return (T)children.get(i);
		for (int i = 0, n = children.size; i < n; i++) {
			Element child = children.get(i);
			if (child instanceof Group) {
				Element actor = ((Group)child).find(name);
				if (actor != null) return (T)actor;
			}
		}
		return null;
	}
	
	/**Find element by a predicate.*/
	public <T extends Element> T find(Predicate<Element> pred){
		Array<Element> children = this.children;
		for (int i = 0, n = children.size; i < n; i++)
			if (pred.evaluate(children.get(i))) return (T)children.get(i);
		
		for (int i = 0, n = children.size; i < n; i++) {
			Element child = children.get(i);
			if (child instanceof Group) {
				Element actor = ((Group)child).find(pred);
				if (actor != null) return (T)actor;
			}
		}
		return null;
	}

	protected void setScene (Scene stage) {
		super.setScene(stage);
		Element[] childrenArray = children.items;
		for (int i = 0, n = children.size; i < n; i++)
			childrenArray[i].setScene(stage); // StackOverflowError here means the group is its own ancestor.
	}

	/** Swaps two actors by index. Returns false if the swap did not occur because the indexes were out of bounds. */
	public boolean swapActor (int first, int second) {
		int maxIndex = children.size;
		if (first < 0 || first >= maxIndex) return false;
		if (second < 0 || second >= maxIndex) return false;
		children.swap(first, second);
		return true;
	}

	/** Swaps two actors. Returns false if the swap did not occur because the actors are not children of this group. */
	public boolean swapActor (Element first, Element second) {
		int firstIndex = children.indexOf(first, true);
		int secondIndex = children.indexOf(second, true);
		if (firstIndex == -1 || secondIndex == -1) return false;
		children.swap(firstIndex, secondIndex);
		return true;
	}

	/** Returns an ordered list of child actors in this group. */
	public SnapshotArray<Element> getChildren () {
		return children;
	}

	public boolean hasChildren () {
		return children.size > 0;
	}

	/** When true (the default), the Batch is transformed so children are drawn in their parent's coordinate system. This has a
	 * performance impact because {@link Batch#flush()} must be done before and after the transform. If the actors in a group are
	 * not rotated or scaled, then the transform for the group can be set to false. In this case, each child's position will be
	 * offset by the group's position for drawing, causing the children to appear in the correct location even though the Batch has
	 * not been transformed. */
	public void setTransform (boolean transform) {
		this.transform = transform;
	}

	public boolean isTransform () {
		return transform;
	}

	/** Converts coordinates for this group to those of a descendant actor. The descendant does not need to be a direct child.
	 * @throws IllegalArgumentException if the specified actor is not a descendant of this group. */
	public Vector2 localToDescendantCoordinates (Element descendant, Vector2 localCoords) {
		Group parent = descendant.parent;
		if (parent == null) throw new IllegalArgumentException("Child is not a descendant: " + descendant);
		// First convert to the actor's parent coordinates.
		if (parent != this) localToDescendantCoordinates(parent, localCoords);
		// Then from each parent down to the descendant.
		descendant.parentToLocalCoordinates(localCoords);
		return localCoords;
	}

	/** If true, {@link #drawDebug(ShapeRenderer)} will be called for this group and, optionally, all children recursively. */
	public void setDebug (boolean enabled, boolean recursively) {
		setDebug(enabled);
		if (recursively) {
			for (Element child : children) {
				if (child instanceof Group) {
					((Group)child).setDebug(enabled, recursively);
				} else {
					child.setDebug(enabled);
				}
			}
		}
	}

	/** Calls {@link #setDebug(boolean, boolean)} with {@code true, true}. */
	public Group debugAll () {
		setDebug(true, true);
		return this;
	}

	/** Returns a description of the actor hierarchy, recursively. */
	public String toString () {
		StringBuilder buffer = new StringBuilder(128);
		toString(buffer, 1);
		buffer.setLength(buffer.length() - 1);
		return buffer.toString();
	}

	void toString (StringBuilder buffer, int indent) {
		buffer.append(super.toString());
		buffer.append('\n');

		Element[] actors = children.begin();
		for (int i = 0, n = children.size; i < n; i++) {
			for (int ii = 0; ii < indent; ii++)
				buffer.append("|  ");
			Element actor = actors[i];
			if (actor instanceof Group)
				((Group)actor).toString(buffer, indent + 1);
			else {
				buffer.append(actor);
				buffer.append('\n');
			}
		}
		children.end();
	}
}
