package io.anuke.ucore.scene.ui;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Inputs.Axis;
import io.anuke.ucore.core.Inputs.ControllerType;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.Inputs.InputDevice;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.core.KeyBinds.Keybind;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Input;
import io.anuke.ucore.util.Input.Type;
import io.anuke.ucore.util.Strings;

public class KeybindDialog extends Dialog{
	protected KeybindDialogStyle style;

	protected KeyBinds.Section section;
	protected String rebindKey = null;
	protected boolean rebindAxis = false;
	protected boolean rebindMin = true;
	protected Dialog rebindDialog;
	protected ObjectIntMap<KeyBinds.Section> sectionControls = new ObjectIntMap<>();

	public KeybindDialog() {
		super(Bundles.get("text.keybind.title", "Rebind Keys"));
		style = Core.skin.get(KeybindDialogStyle.class);
		setup();
		addCloseButton();

		Iterable<KeyBinds.Section> sections = KeyBinds.getSections();
		section = sections.iterator().next();

		Controllers.addListener(new ControllerAdapter(){
			public void connected(Controller controller){
				setup();
			}

			public void disconnected(Controller controller){
				setup();
			}

			public boolean buttonDown (Controller controller, int buttonIndex) {
				if(canRebindController()){
					rebind(Input.findByType(Type.controller, buttonIndex, false));
					return false;
				}
				return false;
			}

			public boolean axisMoved (Controller controller, int axisIndex, float value) {
				if(canRebindController() && rebindAxis && Math.abs(value) > 0.5f){
					rebind(Input.findByType(Type.controller, axisIndex, true));
					return false;
				}
				return false;
			}

			@Override
			public boolean povMoved(Controller controller, int povIndex, PovDirection value) {
				if(canRebindController() && value != PovDirection.center){
					rebind(Input.findPOV(value));
					return false;
				}
				return super.povMoved(controller, povIndex, value);
			}
		});

	}

	public void setStyle(KeybindDialogStyle style){
		this.style = style;
		setup();
	}

	private void setup(){
		content().clear();

		Array<KeyBinds.Section> sections = KeyBinds.getSections();

		Stack stack = new Stack();
		ButtonGroup<TextButton> group = new ButtonGroup<>();
		ScrollPane pane = new ScrollPane(stack, style.paneStyle);
		pane.setFadeScrollBars(false);

		for(KeyBinds.Section section : sections){
			if(!sectionControls.containsKey(section))
				sectionControls.put(section, Inputs.getDevices().indexOf(section.device, true));

			if(sectionControls.get(section, 0) >= Inputs.getDevices().size){
				sectionControls.put(section, 0);
				section.device = Inputs.getDevices().get(0);
			}

			if(sections.size != 1){
				TextButton button = new TextButton(Strings.capitalize(section.name), "toggle");
				if(section.equals(this.section))
					button.toggle();

				button.clicked(() -> {
					this.section = section;
				});

				group.add(button);
				content().add(button).fill();
			}

			Table table = new Table();

			Label device = new Label("Keyboard");
			//device.setColor(style.controllerColor);
			device.setAlignment(Align.center);

			Array<InputDevice> devices = Inputs.getDevices();
			
			Table stable = new Table();

			stable.addButton("<", () -> {
				int i = sectionControls.get(section, 0);
				if(i - 1 >= 0){
					sectionControls.put(section, i - 1);
					section.device = devices.get(i - 1);
					KeyBinds.save();
					setup();
				}
			}).disabled(sectionControls.get(section, 0) - 1 < 0).size(40);

			stable.add(device).minWidth(device.getMinWidth() + 60);

			device.setText(Inputs.getDevices().get(sectionControls.get(section, 0)).name);

			stable.addButton(">", () -> {
				int i = sectionControls.get(section, 0);

				if(i + 1 < devices.size){
					sectionControls.put(section, i + 1);
					section.device = devices.get(i + 1);
					KeyBinds.save();
					setup();
				}
			}).disabled(sectionControls.get(section, 0) + 1 >= devices.size).size(40);

			table.add(stable).colspan(3);

			table.row();
			table.add().height(10);
			table.row();
			if(section.device.controller != null){
				table.table(info -> {
					if(section.device.controllerType == ControllerType.unknown){
						info.add("Controller Type: [RED]unsupported").left();
					}else {
						info.add("Controller Type: [#" + style.controllerColor.toString().toUpperCase() + "]" + Strings.capitalize(section.device.controllerType.name())).left();
					}
				});
			}
			table.row();

			for(Keybind keybind : section.keybinds.get(section.device.type)){
				String key = keybind.name;
				if(keybind.isAxis()) {
					Axis def = keybind.axis;
					Axis axis = section.axisBinds.get(section.device.type).get(keybind.name);
					table.add(Strings.capitalize(Bundles.get("keybind."+key+".name", key)), style.keyNameColor).left().padRight(40).padLeft(8);

					if(axis.min.axis){
						table.add(axis.min.toString(), style.keyColor).left().minWidth(90).padRight(20);
					}else{
						Table axt = new Table();
						axt.left();
						axt.labelWrap(axis.min.toString() + " [red]/[] " + axis.max.toString()).color(style.keyColor).width(140f).padRight(5);
						table.add(axt).left().minWidth(90).padRight(20);
					}

					table.addButton("Rebind", () ->{
						rebindAxis = true;
						rebindMin = true;
						openDialog(section, key);
					});
					table.row();
				}else{
					table.add(Strings.capitalize(key), style.keyNameColor).left().padRight(40).padLeft(8);
					table.add(section.binds.get(section.device.type).get(key, keybind.input).toString(), style.keyColor).left().minWidth(90).padRight(20);
					table.addButton("Rebind", () -> openDialog(section, key));
					table.row();
				}
			}

			table.setVisible(() -> this.section.equals(section));

			table.addButton("Reset to Defaults", () -> {
				KeyBinds.resetToDefaults();
				setup();
				KeyBinds.save();
			}).colspan(4).padTop(4).fill();

			stack.add(table);
		}

		content().row();

		content().add(pane).growX().colspan(sections.size);

		pack();
	}

	/** Internal use only! */
	private boolean canRebindController(){
		return rebindKey != null && section.device.type == DeviceType.controller;
	}

	/** Internal use only! */
	private void rebind(Input input){
		rebindDialog.hide();

		if(rebindAxis){
			Axis axis = section.axisBinds.get(section.device.type).get(rebindKey);
			if(input.axis){
				axis.min = input;
			}else{
				if(rebindMin){
					axis.min = input;
				}else{
					axis.max = input;
				}
			}
		}else{
			section.binds.get(section.device.type).put(rebindKey, input);
		}

		if(rebindAxis && !input.axis && rebindMin){
			rebindMin = false;
			openDialog(section, rebindKey);
		}else{
			KeyBinds.save();
			rebindKey = null;
			rebindAxis = false;
			setup();
		}
	}

	private void openDialog(KeyBinds.Section section, String name){
		//boolean rebindTwo = section.device.type != DeviceType.controller;

		rebindDialog = new Dialog(rebindAxis ? "Press an axis or key..." : "Press a key...", "dialog");

		rebindKey = name;

		rebindDialog.getTitleTable().getCells().first().pad(4);
		//rebindDialog.addButton("Cancel", rebindDialog::hide).pad(4);

		if(section.device.type == DeviceType.keyboard) {
			rebindDialog.keyDown(i -> {
				setup();
			});

			rebindDialog.addListener(new InputListener(){
				public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
					if(Gdx.app.getType() == ApplicationType.Android) return false;
					rebind(Input.findByType(Type.mouse, button, false));
					return false;
				}

				public boolean keyDown (InputEvent event, int keycode) {
					rebindDialog.hide();
					if(keycode == Keys.ESCAPE) return false;
					rebind(Input.findByType(Type.key, keycode, false));
					return false;
				}

                @Override
                public boolean scrolled(InputEvent event, float x, float y, int amount) {
				    if(!rebindAxis) return false;
					rebindDialog.hide();
                    rebind(Input.SCROLL);
				    return false;
                }
            });
		}

		rebindDialog.show();
        Timers.runTask(1f, () -> {
            getScene().setScrollFocus(rebindDialog);
        });
	}

	static public class KeybindDialogStyle{
		public Color keyColor = Color.WHITE;
		public Color keyNameColor = Color.WHITE;
		public Color controllerColor = Color.WHITE;
		public String paneStyle = "default";
	}
}
