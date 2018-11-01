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

import com.badlogic.gdx.*;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.scene.event.*;
import io.anuke.ucore.scene.event.InputEvent.Type;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Table.Debug;
import io.anuke.ucore.scene.event.FocusListener.FocusEvent;
import io.anuke.ucore.scene.utils.ScissorStack;

/** A 2D scene graph containing hierarchies of {@link Element actors}. Stage handles the viewport and distributes input events.
 * <p>
 * {@link #setViewport(Viewport)} controls the coordinates used within the stage and sets up the camera used to convert between
 * stage coordinates and screen coordinates.
 * <p>
 * A stage must receive input events so it can distribute them to actors. This is typically done by passing the stage to
 * {@link Input#setInputProcessor(com.badlogic.gdx.InputProcessor) Gdx.input.setInputProcessor}. An {@link InputMultiplexer} may
 * be used to handle input events before or after the stage does. If an actor handles an event by returning true from the input
 * method, then the stage's input method will also return true, causing subsequent InputProcessors to not receive the event.
 * <p>
 * The Stage and its constituents (like Actors and Listeners) are not thread-safe and should only be updated and queried from a
 * single thread (presumably the main render thread). Methods should be reentrant, so you can update Actors and Stages from within
 * callbacks and handlers.
 * @author mzechner
 * @author Nathan Sweet */
public class Scene extends InputAdapter implements Disposable {
	/** True if any actor has ever had debug enabled. */
	static boolean debug;

	private Viewport viewport;
	private final Batch batch;
	private boolean ownsBatch;
	private Group root;
	private final Vector2 tempCoords = new Vector2();
	private final Element[] pointerOverActors = new Element[20];
	private final boolean[] pointerTouched = new boolean[20];
	private final int[] pointerScreenX = new int[20];
	private final int[] pointerScreenY = new int[20];
	private int mouseScreenX, mouseScreenY;
	private Element mouseOverActor;
	private Element keyboardFocus, scrollFocus;
	private final SnapshotArray<TouchFocus> touchFocuses = new SnapshotArray(true, 4, TouchFocus.class);
	private boolean actionsRequestRendering = true;

	private ShapeRenderer debugShapes;
	private boolean debugInvisible, debugAll, debugUnderMouse, debugParentUnderMouse;
	private Debug debugTableUnderMouse = Debug.none;
	private final Color debugColor = new Color(0, 1, 0, 0.85f);

	/** Creates a stage with a {@link ScalingViewport} set to {@link Scaling#stretch}. The stage will use its own {@link Batch}
	 * which will be disposed when the stage is disposed. */
	public Scene () {
		this(new SpriteBatch());
		System.out.println("construct invoked");
		ownsBatch = true;
	}

	/** Creates a stage with the specified viewport. The stage will use its own {@link Batch} which will be disposed when the stage
	 * is disposed. */
	public Scene (Viewport viewport) {
		this(new SpriteBatch());
		ownsBatch = true;
	}

	/** Creates a stage with the specified viewport and batch. This can be used to avoid creating a new batch (which can be
	 * somewhat slow) if multiple stages are used during an application's life time.
	 * @param batch Will not be disposed if {@link #dispose()} is called, handle disposal yourself. */
	public Scene (Batch batch) {
		if (batch == null) throw new IllegalArgumentException("batch cannot be null.");

		this.batch = batch;
		this.viewport = new ScreenViewport(){
			@Override
			public void calculateScissors (Matrix4 batchTransform, Rectangle area, Rectangle scissor) {
				ScissorStack.calculateScissors(
						getCamera(), getScreenX(), getScreenY(), getScreenWidth(), getScreenHeight(), batchTransform, area, scissor);
			}
		};

		root = new Group();
		root.setScene(this);

		viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
	}

	public void draw () {
		Camera camera = viewport.getCamera();
		camera.update();

		if (!root.isVisible()) return;
		
		Batch old = Core.batch;
		Core.batch = (SpriteBatch)batch;
		
		Batch batch = this.batch;
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		root.draw(batch, 1);
		batch.end();
		
		Core.batch = old;

		if (debug) drawDebug();
	}

	private void drawDebug () {
		if (debugShapes == null) {
			debugShapes = new ShapeRenderer();
			debugShapes.setAutoShapeType(true);
		}

		if (debugUnderMouse || debugParentUnderMouse || debugTableUnderMouse != Debug.none) {
			screenToStageCoordinates(tempCoords.set(Gdx.input.getX(), Gdx.input.getY()));
			Element actor = hit(tempCoords.x, tempCoords.y, true);
			if (actor == null) return;

			if (debugParentUnderMouse && actor.parent != null) actor = actor.parent;

			if (debugTableUnderMouse == Debug.none)
				actor.setDebug(true);
			else {
				while (actor != null) {
					if (actor instanceof Table) break;
					actor = actor.parent;
				}
				if (actor == null) return;
				((Table)actor).debug(debugTableUnderMouse);
			}

			if (debugAll && actor instanceof Group) ((Group)actor).debugAll();

			disableDebug(root, actor);
		} else {
			if (debugAll) root.debugAll();
		}

		Gdx.gl.glEnable(GL20.GL_BLEND);
		debugShapes.setProjectionMatrix(viewport.getCamera().combined);
		debugShapes.begin();
		root.drawDebug(debugShapes);
		debugShapes.end();
	}

	/** Disables debug on all actors recursively except the specified actor and any children. */
	private void disableDebug (Element actor, Element except) {
		if (actor == except) return;
		actor.setDebug(false);
		if (actor instanceof Group) {
			SnapshotArray<Element> children = ((Group)actor).children;
			for (int i = 0, n = children.size; i < n; i++)
				disableDebug(children.get(i), except);
		}
	}

	/** Calls {@link #act(float)} with {@link Graphics#getDeltaTime()}, limited to a minimum of 30fps. */
	public void act () {
		act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
	}

	/** Calls the {@link Element#act(float)} method on each actor in the stage. Typically called each frame. This method also fires
	 * enter and exit events.
	 * @param delta Time in seconds since the last frame. */
	public void act (float delta) {
		// Update over actors. Done in act() because actors may change position, which can fire enter/exit without an input event.
		for (int pointer = 0, n = pointerOverActors.length; pointer < n; pointer++) {
			Element overLast = pointerOverActors[pointer];
			// Check if pointer is gone.
			if (!pointerTouched[pointer]) {
				if (overLast != null) {
					pointerOverActors[pointer] = null;
					screenToStageCoordinates(tempCoords.set(pointerScreenX[pointer], pointerScreenY[pointer]));
					// Exit over last.
					InputEvent event = Pools.obtain(InputEvent.class);
					event.setType(InputEvent.Type.exit);
					event.setStage(this);
					event.setStageX(tempCoords.x);
					event.setStageY(tempCoords.y);
					event.setRelatedActor(overLast);
					event.setPointer(pointer);
					overLast.fire(event);
					Pools.free(event);
				}
				continue;
			}
			// Update over actor for the pointer.
			pointerOverActors[pointer] = fireEnterAndExit(overLast, pointerScreenX[pointer], pointerScreenY[pointer], pointer);
		}
		// Update over actor for the mouse on the desktop.
		ApplicationType type = Gdx.app.getType();
		if (type == ApplicationType.Desktop || type == ApplicationType.Applet || type == ApplicationType.WebGL)
			mouseOverActor = fireEnterAndExit(mouseOverActor, mouseScreenX, mouseScreenY, -1);

		root.act(delta);
	}
	
	public Element find(String name){
		return root.find(name);
	}
	
	public Element find(Predicate<Element> pred){
		return root.find(pred);
	}
	
	/**Adds and returns a table.*/
	public Table table(){
		Table table = new Table();
		table.setFillParent(true);
		add(table);
		return table;
	}

	private Element fireEnterAndExit (Element overLast, int screenX, int screenY, int pointer) {
		// Find the actor under the point.
		screenToStageCoordinates(tempCoords.set(screenX, screenY));
		Element over = hit(tempCoords.x, tempCoords.y, true);
		if (over == overLast) return overLast;

		// Exit overLast.
		if (overLast != null) {
			InputEvent event = Pools.obtain(InputEvent.class);
			event.setStage(this);
			event.setStageX(tempCoords.x);
			event.setStageY(tempCoords.y);
			event.setPointer(pointer);
			event.setType(InputEvent.Type.exit);
			event.setRelatedActor(over);
			overLast.fire(event);
			Pools.free(event);
		}
		// Enter over.
		if (over != null) {
			InputEvent event = Pools.obtain(InputEvent.class);
			event.setStage(this);
			event.setStageX(tempCoords.x);
			event.setStageY(tempCoords.y);
			event.setPointer(pointer);
			event.setType(InputEvent.Type.enter);
			event.setRelatedActor(overLast);
			over.fire(event);
			Pools.free(event);
		}
		return over;
	}

	/** Applies a touch down event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the
	 * event. */
	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		if (!isInsideViewport(screenX, screenY)) return false;

		pointerTouched[pointer] = true;
		pointerScreenX[pointer] = screenX;
		pointerScreenY[pointer] = screenY;

		screenToStageCoordinates(tempCoords.set(screenX, screenY));

		InputEvent event = Pools.obtain(InputEvent.class);
		event.setType(Type.touchDown);
		event.setStage(this);
		event.setStageX(tempCoords.x);
		event.setStageY(tempCoords.y);
		event.setPointer(pointer);
		event.setButton(button);

		Element target = hit(tempCoords.x, tempCoords.y, true);
		if (target == null) {
			if (root.getTouchable() == Touchable.enabled) root.fire(event);
		} else {
			target.fire(event);
		}

		boolean handled = event.isHandled();
		Pools.free(event);
		return handled;
	}

	/** Applies a touch moved event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the
	 * event. Only {@link InputListener listeners} that returned true for touchDown will receive this event. */
	public boolean touchDragged (int screenX, int screenY, int pointer) {
		pointerScreenX[pointer] = screenX;
		pointerScreenY[pointer] = screenY;
		mouseScreenX = screenX;
		mouseScreenY = screenY;

		if (touchFocuses.size == 0) return false;

		screenToStageCoordinates(tempCoords.set(screenX, screenY));

		InputEvent event = Pools.obtain(InputEvent.class);
		event.setType(Type.touchDragged);
		event.setStage(this);
		event.setStageX(tempCoords.x);
		event.setStageY(tempCoords.y);
		event.setPointer(pointer);

		SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
		TouchFocus[] focuses = touchFocuses.begin();
		for (int i = 0, n = touchFocuses.size; i < n; i++) {
			TouchFocus focus = focuses[i];
			if (focus.pointer != pointer) continue;
			if (!touchFocuses.contains(focus, true)) continue; // Touch focus already gone.
			event.setTarget(focus.target);
			event.setListenerActor(focus.listenerActor);
			if (focus.listener.handle(event)) event.handle();
		}
		touchFocuses.end();

		boolean handled = event.isHandled();
		Pools.free(event);
		return handled;
	}

	/** Applies a touch up event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the event.
	 * Only {@link InputListener listeners} that returned true for touchDown will receive this event. */
	public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		pointerTouched[pointer] = false;
		pointerScreenX[pointer] = screenX;
		pointerScreenY[pointer] = screenY;

		if (touchFocuses.size == 0) return false;

		screenToStageCoordinates(tempCoords.set(screenX, screenY));

		InputEvent event = Pools.obtain(InputEvent.class);
		event.setType(Type.touchUp);
		event.setStage(this);
		event.setStageX(tempCoords.x);
		event.setStageY(tempCoords.y);
		event.setPointer(pointer);
		event.setButton(button);

		SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
		TouchFocus[] focuses = touchFocuses.begin();
		for (int i = 0, n = touchFocuses.size; i < n; i++) {
			TouchFocus focus = focuses[i];
			if (focus.pointer != pointer || focus.button != button) continue;
			if (!touchFocuses.removeValue(focus, true)) continue; // Touch focus already gone.
			event.setTarget(focus.target);
			event.setListenerActor(focus.listenerActor);
			if (focus.listener.handle(event)) event.handle();
			Pools.free(focus);
		}
		touchFocuses.end();

		boolean handled = event.isHandled();
		Pools.free(event);
		return handled;
	}

	/** Applies a mouse moved event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the
	 * event. This event only occurs on the desktop. */
	public boolean mouseMoved (int screenX, int screenY) {
		if (!isInsideViewport(screenX, screenY)) return false;

		mouseScreenX = screenX;
		mouseScreenY = screenY;

		screenToStageCoordinates(tempCoords.set(screenX, screenY));

		InputEvent event = Pools.obtain(InputEvent.class);
		event.setStage(this);
		event.setType(Type.mouseMoved);
		event.setStageX(tempCoords.x);
		event.setStageY(tempCoords.y);

		Element target = hit(tempCoords.x, tempCoords.y, true);
		if (target == null) target = root;

		target.fire(event);
		boolean handled = event.isHandled();
		Pools.free(event);
		return handled;
	}

	/** Applies a mouse scroll event to the stage and returns true if an actor in the scene {@link Event#handle() handled} the
	 * event. This event only occurs on the desktop. */
	public boolean scrolled (int amount) {
		Element target = scrollFocus == null ? root : scrollFocus;

		screenToStageCoordinates(tempCoords.set(mouseScreenX, mouseScreenY));

		InputEvent event = Pools.obtain(InputEvent.class);
		event.setStage(this);
		event.setType(InputEvent.Type.scrolled);
		event.setScrollAmount(amount);
		event.setStageX(tempCoords.x);
		event.setStageY(tempCoords.y);
		target.fire(event);
		boolean handled = event.isHandled();
		Pools.free(event);
		return handled;
	}

	/** Applies a key down event to the actor that has {@link Scene#setKeyboardFocus(Element) keyboard focus}, if any, and returns
	 * true if the event was {@link Event#handle() handled}. */
	public boolean keyDown (int keyCode) {
		Element target = keyboardFocus == null ? root : keyboardFocus;
		InputEvent event = Pools.obtain(InputEvent.class);
		event.setStage(this);
		event.setType(InputEvent.Type.keyDown);
		event.setKeyCode(keyCode);
		target.fire(event);
		boolean handled = event.isHandled();
		Pools.free(event);
		return handled;
	}

	/** Applies a key up event to the actor that has {@link Scene#setKeyboardFocus(Element) keyboard focus}, if any, and returns true
	 * if the event was {@link Event#handle() handled}. */
	public boolean keyUp (int keyCode) {
		Element target = keyboardFocus == null ? root : keyboardFocus;
		InputEvent event = Pools.obtain(InputEvent.class);
		event.setStage(this);
		event.setType(InputEvent.Type.keyUp);
		event.setKeyCode(keyCode);
		target.fire(event);
		boolean handled = event.isHandled();
		Pools.free(event);
		return handled;
	}

	/** Applies a key typed event to the actor that has {@link Scene#setKeyboardFocus(Element) keyboard focus}, if any, and returns
	 * true if the event was {@link Event#handle() handled}. */
	public boolean keyTyped (char character) {
		Element target = keyboardFocus == null ? root : keyboardFocus;
		InputEvent event = Pools.obtain(InputEvent.class);
		event.setStage(this);
		event.setType(InputEvent.Type.keyTyped);
		event.setCharacter(character);
		target.fire(event);
		boolean handled = event.isHandled();
		Pools.free(event);
		return handled;
	}

	/** Adds the listener to be notified for all touchDragged and touchUp events for the specified pointer and button. The actor
	 * will be used as the {@link Event#getListenerActor() listener actor} and {@link Event#getTarget() target}. */
	public void addTouchFocus (EventListener listener, Element listenerActor, Element target, int pointer, int button) {
		TouchFocus focus = Pools.obtain(TouchFocus.class);
		focus.listenerActor = listenerActor;
		focus.target = target;
		focus.listener = listener;
		focus.pointer = pointer;
		focus.button = button;
		touchFocuses.add(focus);
	}

	/** Removes the listener from being notified for all touchDragged and touchUp events for the specified pointer and button. Note
	 * the listener may never receive a touchUp event if this method is used. */
	public void removeTouchFocus (EventListener listener, Element listenerActor, Element target, int pointer, int button) {
		SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
		for (int i = touchFocuses.size - 1; i >= 0; i--) {
			TouchFocus focus = touchFocuses.get(i);
			if (focus.listener == listener && focus.listenerActor == listenerActor && focus.target == target
				&& focus.pointer == pointer && focus.button == button) {
				touchFocuses.removeIndex(i);
				Pools.free(focus);
			}
		}
	}

	/** Cancels touch focus for the specified actor.
	 * @see #cancelTouchFocus() */
	public void cancelTouchFocus (Element actor) {
		InputEvent event = Pools.obtain(InputEvent.class);
		event.setStage(this);
		event.setType(InputEvent.Type.touchUp);
		event.setStageX(Integer.MIN_VALUE);
		event.setStageY(Integer.MIN_VALUE);

		// Cancel all current touch focuses for the specified listener, allowing for concurrent modification, and never cancel the
		// same focus twice.
		SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
		TouchFocus[] items = touchFocuses.begin();
		for (int i = 0, n = touchFocuses.size; i < n; i++) {
			TouchFocus focus = items[i];
			if (focus.listenerActor != actor) continue;
			if (!touchFocuses.removeValue(focus, true)) continue; // Touch focus already gone.
			event.setTarget(focus.target);
			event.setListenerActor(focus.listenerActor);
			event.setPointer(focus.pointer);
			event.setButton(focus.button);
			focus.listener.handle(event);
			// Cannot return TouchFocus to pool, as it may still be in use (eg if cancelTouchFocus is called from touchDragged).
		}
		touchFocuses.end();

		Pools.free(event);
	}

	/** Sends a touchUp event to all listeners that are registered to receive touchDragged and touchUp events and removes their
	 * touch focus. This method removes all touch focus listeners, but sends a touchUp event so that the state of the listeners
	 * remains consistent (listeners typically expect to receive touchUp eventually). The location of the touchUp is
	 * {@link Integer#MIN_VALUE}. Listeners can use {@link InputEvent#isTouchFocusCancel()} to ignore this event if needed. */
	public void cancelTouchFocus () {
		cancelTouchFocusExcept(null, null);
	}

	/** Cancels touch focus for all listeners except the specified listener.
	 * @see #cancelTouchFocus() */
	public void cancelTouchFocusExcept (EventListener exceptListener, Element exceptActor) {
		InputEvent event = Pools.obtain(InputEvent.class);
		event.setStage(this);
		event.setType(InputEvent.Type.touchUp);
		event.setStageX(Integer.MIN_VALUE);
		event.setStageY(Integer.MIN_VALUE);

		// Cancel all current touch focuses except for the specified listener, allowing for concurrent modification, and never
		// cancel the same focus twice.
		SnapshotArray<TouchFocus> touchFocuses = this.touchFocuses;
		TouchFocus[] items = touchFocuses.begin();
		for (int i = 0, n = touchFocuses.size; i < n; i++) {
			TouchFocus focus = items[i];
			if (focus.listener == exceptListener && focus.listenerActor == exceptActor) continue;
			if (!touchFocuses.removeValue(focus, true)) continue; // Touch focus already gone.
			event.setTarget(focus.target);
			event.setListenerActor(focus.listenerActor);
			event.setPointer(focus.pointer);
			event.setButton(focus.button);
			focus.listener.handle(event);
			// Cannot return TouchFocus to pool, as it may still be in use (eg if cancelTouchFocus is called from touchDragged).
		}
		touchFocuses.end();

		Pools.free(event);
	}

	/** Adds an actor to the root of the stage.
	 * @see Group#addChild(Element) */
	public void add (Element actor) {
		root.addChild(actor);
	}

	/** Adds an action to the root of the stage.
	 * @see Group#addAction(Action) */
	public void addAction (Action action) {
		root.addAction(action);
	}

	/** Returns the root's child actors.
	 * @see Group#getChildren() */
	public Array<Element> getElements () {
		return root.children;
	}

	/** Adds a listener to the root.
	 * @see Element#addListener(EventListener) */
	public boolean addListener (EventListener listener) {
		return root.addListener(listener);
	}

	/** Removes a listener from the root.
	 * @see Element#removeListener(EventListener) */
	public boolean removeListener (EventListener listener) {
		return root.removeListener(listener);
	}

	/** Adds a capture listener to the root.
	 * @see Element#addCaptureListener(EventListener) */
	public boolean addCaptureListener (EventListener listener) {
		return root.addCaptureListener(listener);
	}

	/** Removes a listener from the root.
	 * @see Element#removeCaptureListener(EventListener) */
	public boolean removeCaptureListener (EventListener listener) {
		return root.removeCaptureListener(listener);
	}

	/** Removes the root's children, actions, and listeners. */
	public void clear () {
		unfocusAll();
		root.clear();
	}

	/** Removes the touch, keyboard, and scroll focused actors. */
	public void unfocusAll () {
		setScrollFocus(null);
		setKeyboardFocus(null);
		cancelTouchFocus();
	}

	/** Removes the touch, keyboard, and scroll focus for the specified actor and any descendants. */
	public void unfocus (Element actor) {
		cancelTouchFocus(actor);
		if (scrollFocus != null && scrollFocus.isDescendantOf(actor)) setScrollFocus(null);
		if (keyboardFocus != null && keyboardFocus.isDescendantOf(actor)) setKeyboardFocus(null);
	}

	/** Sets the actor that will receive key events.
	 * @param actor May be null.
	 * @return true if the unfocus and focus events were not cancelled by a {@link FocusListener}. */
	public boolean setKeyboardFocus (Element actor) {
		if (keyboardFocus == actor) return true;
		FocusEvent event = Pools.obtain(FocusEvent.class);
		event.setStage(this);
		event.setType(FocusEvent.Type.keyboard);
		Element oldKeyboardFocus = keyboardFocus;
		if (oldKeyboardFocus != null) {
			event.setFocused(false);
			event.setRelatedActor(actor);
			oldKeyboardFocus.fire(event);
		}
		boolean success = !event.isCancelled();
		if (success) {
			keyboardFocus = actor;
			if (actor != null) {
				event.setFocused(true);
				event.setRelatedActor(oldKeyboardFocus);
				actor.fire(event);
				success = !event.isCancelled();
				if (!success) setKeyboardFocus(oldKeyboardFocus);
			}
		}
		Pools.free(event);
		return success;
	}

	/** Gets the actor that will receive key events.
	 * @return May be null. */
	public Element getKeyboardFocus () {
		return keyboardFocus;
	}

	/** Sets the actor that will receive scroll events.
	 * @param actor May be null.
	 * @return true if the unfocus and focus events were not cancelled by a {@link FocusListener}. */
	public boolean setScrollFocus (Element actor) {
		if (scrollFocus == actor) return true;
		FocusEvent event = Pools.obtain(FocusEvent.class);
		event.setStage(this);
		event.setType(FocusEvent.Type.scroll);
		Element oldScrollFocus = scrollFocus;
		if (oldScrollFocus != null) {
			event.setFocused(false);
			event.setRelatedActor(actor);
			oldScrollFocus.fire(event);
		}
		boolean success = !event.isCancelled();
		if (success) {
			scrollFocus = actor;
			if (actor != null) {
				event.setFocused(true);
				event.setRelatedActor(oldScrollFocus);
				actor.fire(event);
				success = !event.isCancelled();
				if (!success) setScrollFocus(oldScrollFocus);
			}
		}
		Pools.free(event);
		return success;
	}

	/** Gets the actor that will receive scroll events.
	 * @return May be null. */
	public Element getScrollFocus () {
		return scrollFocus;
	}

	public Batch getBatch () {
		return batch;
	}

	public Viewport getViewport () {
		return viewport;
	}

	public void setViewport (Viewport viewport) {
		this.viewport = viewport;
	}

	/** The viewport's world width. */
	public float getWidth () {
		return viewport.getWorldWidth();
	}

	/** The viewport's world height. */
	public float getHeight () {
		return viewport.getWorldHeight();
	}

	/** The viewport's camera. */
	public Camera getCamera () {
		return viewport.getCamera();
	}

	/** Returns the root group which holds all actors in the stage. */
	public Group getRoot () {
		return root;
	}

	/** Replaces the root group. Usually this is not necessary but a subclass may be desired in some cases, eg being notified of
	 * {@link Group#childrenChanged()}. */
	public void setRoot (Group root) {
		this.root = root;
	}

	/** Returns the {@link Element} at the specified location in stage coordinates. Hit testing is performed in the order the actors
	 * were inserted into the stage, last inserted actors being tested first. To get stage coordinates from screen coordinates, use
	 * {@link #screenToStageCoordinates(Vector2)}.
	 * @param touchable If true, the hit detection will respect the {@link Element#setTouchable(Touchable) touchability}.
	 * @return May be null if no actor was hit. */
	public Element hit (float stageX, float stageY, boolean touchable) {
		root.parentToLocalCoordinates(tempCoords.set(stageX, stageY));
		return root.hit(tempCoords.x, tempCoords.y, touchable);
	}

	/** Transforms the screen coordinates to stage coordinates.
	 * @param screenCoords Input screen coordinates and output for resulting stage coordinates. */
	public Vector2 screenToStageCoordinates (Vector2 screenCoords) {
		viewport.unproject(screenCoords);
		return screenCoords;
	}

	/** Transforms the stage coordinates to screen coordinates.
	 * @param stageCoords Input stage coordinates and output for resulting screen coordinates. */
	public Vector2 stageToScreenCoordinates (Vector2 stageCoords) {
		viewport.project(stageCoords);
		stageCoords.y = viewport.getScreenHeight() - stageCoords.y;
		return stageCoords;
	}

	/** Transforms the coordinates to screen coordinates. The coordinates can be anywhere in the stage since the transform matrix
	 * describes how to convert them. The transform matrix is typically obtained from {@link Batch#getTransformMatrix()} during
	 * {@link Element#draw(Batch, float)}.
	 * @see Element#localToStageCoordinates(Vector2) */
	public Vector2 toScreenCoordinates (Vector2 coords, Matrix4 transformMatrix) {
		return viewport.toScreenCoordinates(coords, transformMatrix);
	}

	/** Calculates window scissor coordinates from local coordinates using the batch's current transformation matrix.
	 * @see ScissorStack#calculateScissors(Camera, float, float, float, float, Matrix4, Rectangle, Rectangle) */
	public void calculateScissors (Rectangle localRect, Rectangle scissorRect) {
		viewport.calculateScissors(batch.getTransformMatrix(), localRect, scissorRect);
		Matrix4 transformMatrix;
		if (debugShapes != null && debugShapes.isDrawing())
			transformMatrix = debugShapes.getTransformMatrix();
		else
			transformMatrix = batch.getTransformMatrix();
		viewport.calculateScissors(transformMatrix, localRect, scissorRect);
	}

	/** If true, any actions executed during a call to {@link #act()}) will result in a call to {@link Graphics#requestRendering()}
	 * . Widgets that animate or otherwise require additional rendering may check this setting before calling
	 * {@link Graphics#requestRendering()}. Default is true. */
	public void setActionsRequestRendering (boolean actionsRequestRendering) {
		this.actionsRequestRendering = actionsRequestRendering;
	}

	public boolean getActionsRequestRendering () {
		return actionsRequestRendering;
	}

	/** The default color that can be used by actors to draw debug lines. */
	public Color getDebugColor () {
		return debugColor;
	}

	/** If true, debug lines are shown for actors even when {@link Element#isVisible()} is false. */
	public void setDebugInvisible (boolean debugInvisible) {
		this.debugInvisible = debugInvisible;
	}

	/** If true, debug lines are shown for all actors. */
	public void setDebugAll (boolean debugAll) {
		if (this.debugAll == debugAll) return;
		this.debugAll = debugAll;
		if (debugAll)
			debug = true;
		else
			root.setDebug(false, true);
	}
	
	public boolean isDebugAll () {
		return debugAll;
	}

	/** If true, debug is enabled only for the actor under the mouse. Can be combined with {@link #setDebugAll(boolean)}. */
	public void setDebugUnderMouse (boolean debugUnderMouse) {
		if (this.debugUnderMouse == debugUnderMouse) return;
		this.debugUnderMouse = debugUnderMouse;
		if (debugUnderMouse)
			debug = true;
		else
			root.setDebug(false, true);
	}

	/** If true, debug is enabled only for the parent of the actor under the mouse. Can be combined with
	 * {@link #setDebugAll(boolean)}. */
	public void setDebugParentUnderMouse (boolean debugParentUnderMouse) {
		if (this.debugParentUnderMouse == debugParentUnderMouse) return;
		this.debugParentUnderMouse = debugParentUnderMouse;
		if (debugParentUnderMouse)
			debug = true;
		else
			root.setDebug(false, true);
	}

	/** If not {@link Debug#none}, debug is enabled only for the first ascendant of the actor under the mouse that is a table. Can
	 * be combined with {@link #setDebugAll(boolean)}.
	 * @param debugTableUnderMouse May be null for {@link Debug#none}. */
	public void setDebugTableUnderMouse (Debug debugTableUnderMouse) {
		if (debugTableUnderMouse == null) debugTableUnderMouse = Debug.none;
		if (this.debugTableUnderMouse == debugTableUnderMouse) return;
		this.debugTableUnderMouse = debugTableUnderMouse;
		if (debugTableUnderMouse != Debug.none)
			debug = true;
		else
			root.setDebug(false, true);
	}

	/** If true, debug is enabled only for the first ascendant of the actor under the mouse that is a table. Can be combined with
	 * {@link #setDebugAll(boolean)}. */
	public void setDebugTableUnderMouse (boolean debugTableUnderMouse) {
		setDebugTableUnderMouse(debugTableUnderMouse ? Debug.all : Debug.none);
	}

	public void dispose () {
		clear();
		if (ownsBatch) batch.dispose();
	}

	/** Check if screen coordinates are inside the viewport's screen area. */
	protected boolean isInsideViewport (int screenX, int screenY) {
		int x0 = viewport.getScreenX();
		int x1 = x0 + viewport.getScreenWidth();
		int y0 = viewport.getScreenY();
		int y1 = y0 + viewport.getScreenHeight();
		screenY = Gdx.graphics.getHeight() - screenY;
		return screenX >= x0 && screenX < x1 && screenY >= y0 && screenY < y1;
	}
	
	/**Updates the viewport.*/
	public void resize(int width, int height){
		viewport.update(width, height, true);
	}

	/** Internal class for managing touch focus. Public only for GWT.
	 * @author Nathan Sweet */
	public static final class TouchFocus implements Poolable {
		EventListener listener;
		Element listenerActor, target;
		int pointer, button;

		public void reset () {
			listenerActor = null;
			listener = null;
			target = null;
		}
	}
}
