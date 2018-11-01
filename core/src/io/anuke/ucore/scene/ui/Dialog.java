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

package io.anuke.ucore.scene.ui;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.function.ActionProvider;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.Action;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.event.*;
import io.anuke.ucore.scene.ui.ImageButton.ImageButtonStyle;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

import static io.anuke.ucore.core.Core.skin;
/** Displays a dialog, which is a modal window containing a content table with a button table underneath it.
 * @author Nathan Sweet */ 
public class Dialog extends Window {
	//TODO just make this work properly by calculating padding
	public static float closePadT, closePadR;
	
	private static ActionProvider 
	defaultShowAction = ()-> Actions.sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Interpolation.fade)),
	defaultHideAction = ()-> Actions.fadeOut(0.4f, Interpolation.fade);
	
	Table contentTable, buttonTable;
	Element previousKeyboardFocus, previousScrollFocus;
	FocusListener focusListener;

	protected InputListener ignoreTouchDown = new InputListener() {
		public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
			event.cancel();
			return false;
		}
	};

	public Dialog (String title) {
		super(title, skin.get(WindowStyle.class));
		initialize();
	}

	public Dialog (String title, String windowStyleName) {
		super(title, skin.get(windowStyleName, WindowStyle.class));
		initialize();
	}

	public Dialog (String title, WindowStyle windowStyle) {
		super(title, windowStyle);
		initialize();
	}

	private void initialize () {
		setModal(true);
		setMovable(false);
		setOrigin(Align.center);

		defaults().space(6);
		add(contentTable = new Table()).expand().fill();
		row();
		add(buttonTable = new Table()).fillX();

		contentTable.defaults().space(6);
		buttonTable.defaults().space(6);

		focusListener = new FocusListener() {
			public void keyboardFocusChanged (FocusEvent event, Element actor, boolean focused) {
				if (!focused) focusChanged(event);
			}

			public void scrollFocusChanged (FocusEvent event, Element actor, boolean focused) {
				if (!focused) focusChanged(event);
			}

			private void focusChanged (FocusEvent event) {
				Scene stage = getScene();
				if (isModal && stage != null && stage.getRoot().getChildren().size > 0
					&& stage.getRoot().getChildren().peek() == Dialog.this) { // Dialog is top most actor.
					Element newFocusedActor = event.getRelatedActor();
					if (newFocusedActor != null && !newFocusedActor.isDescendantOf(Dialog.this) &&
						!(newFocusedActor.equals(previousKeyboardFocus) || newFocusedActor.equals(previousScrollFocus)) )
						event.cancel();
				}
			}
		};
	}

	protected void setScene (Scene stage) {
		if (stage == null)
			addListener(focusListener);
		else
			removeListener(focusListener);
		super.setScene(stage);
	}
	
	public void addCloseButton () {
		Label titleLabel = getTitleLabel();
		Table titleTable = getTitleTable();

		ImageButton closeButton = new ImageButton(skin.get("close-window", ImageButtonStyle.class));
		
		float scl = Unit.dp.scl(1f);
		
		titleTable.add(closeButton).padRight(-getMarginRight()/scl)
		.padTop(-10+closePadT).size(40);
		
		closeButton.changed(this::hide);

		if (titleLabel.getLabelAlign() == Align.center && titleTable.getChildren().size == 2){
			titleTable.getCell(titleLabel).padLeft(closeButton.getWidth() * 2);
		}
	}
	
	public Table content(){
		return contentTable;
	}
	
	public Table buttons(){
		return buttonTable;
	}
	
	public Label title(){
		return titleLabel;
	}

	public Table getContentTable () {
		return contentTable;
	}

	public Table getButtonTable () {
		return buttonTable;
	}
	
	/**Adds a show() listener.*/
	public void shown(Listenable run){
		addListener(new VisibilityListener(){
			@Override
			public boolean shown(){
				run.listen();
				return false;
			}
		});
	}

	/**Adds a hide() listener.*/
	public void hidden(Listenable run){
		addListener(new VisibilityListener(){
			@Override
			public boolean hidden(){
				run.listen();
				return false;
			}
		});
	}
	
	/**Sets style to 'dialog'.*/
	public Dialog setDialog(){
		setStyle(skin.get("dialog", WindowStyle.class));
		return this;
	}

	public boolean isShown(){
		return getScene() != null;
	}

	/** {@link #pack() Packs} the dialog and adds it to the stage with custom action which can be null for instant show */
	public Dialog show (Scene stage, Action action) {
		setOrigin(Align.center);
		setClip(false);
		setTransform(true);

		this.fire(new VisibilityEvent(false));

		clearActions();
		removeCaptureListener(ignoreTouchDown);

		previousKeyboardFocus = null;
		Element actor = stage.getKeyboardFocus();
		if (actor != null && !actor.isDescendantOf(this)) previousKeyboardFocus = actor;

		previousScrollFocus = null;
		actor = stage.getScrollFocus();
		if (actor != null && !actor.isDescendantOf(this)) previousScrollFocus = actor;

		pack();
		stage.add(this);
		stage.setKeyboardFocus(this);
		stage.setScrollFocus(this);
		
		if (action != null) addAction(action);

		return this;
	}
	
	/**Shows using the ModuleController's UI.*/
	public Dialog show () {
		return show(Core.scene);
	}

	/** {@link #pack() Packs} the dialog and adds it to the stage, centered with default fadeIn action */
	public Dialog show (Scene stage) {
		show(stage, defaultShowAction.get());
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
		return this;
	}

	/** Hides the dialog with the given action and then removes it from the stage. */
	public void hide (Action action) {
		this.fire(new VisibilityEvent(true));
		
		Scene stage = getScene();
		if (stage != null) {
			removeListener(focusListener);
			if (previousKeyboardFocus != null && previousKeyboardFocus.getScene() == null) previousKeyboardFocus = null;
			Element actor = stage.getKeyboardFocus();
			if (actor == null || actor.isDescendantOf(this)) stage.setKeyboardFocus(previousKeyboardFocus);

			if (previousScrollFocus != null && previousScrollFocus.getScene() == null) previousScrollFocus = null;
			actor = stage.getScrollFocus();
			if (actor == null || actor.isDescendantOf(this)) stage.setScrollFocus(previousScrollFocus);
		}
		if (action != null) {
			addCaptureListener(ignoreTouchDown);
			addAction(Actions.sequence(action, Actions.removeListener(ignoreTouchDown, true), Actions.removeActor()));
		} else
			remove();
	}

	/** Hides the dialog. Called automatically when a button is clicked. The default implementation fades out the dialog over 400
	 * milliseconds. */
	public void hide () {
		setOrigin(Align.center);
		setClip(false);
		setTransform(true);
		
		hide(defaultHideAction.get());
	}
	
	public static ActionProvider getHideAction(){
		return defaultHideAction;
	}
	
	public static ActionProvider getShowAction(){
		return defaultShowAction;
	}
	
	public static void setShowAction(ActionProvider prov){
		defaultShowAction = prov;
	}
	
	public static void setHideAction(ActionProvider prov){
		defaultHideAction = prov;
	}
}
