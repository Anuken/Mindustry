package io.anuke.ucore.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.ucore.core.KeyBinds.Section;
import io.anuke.ucore.util.Input;
import io.anuke.ucore.util.Input.Type;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.OS;

public class Inputs{
	private static boolean useControllers = true;
	private static boolean[] buttons = new boolean[5];
	private static IntSet keysReleased = new IntSet();
	private static InputMultiplexer plex = new InputMultiplexer();
	private static int scroll = 0;
	private static boolean debug = false;
	private static InputProcessor listen = new InputAdapter(){
		@Override
		public boolean scrolled(int amount){
			scroll = -amount;
			return false;
		}

		@Override
		public boolean keyUp(int keycode) {
			keysReleased.add(keycode);
			return false;
		}
	};
	private static Array<InputDevice> devices = new Array<>();
	
	static{
		plex.addProcessor(listen);
		
		devices.add(new InputDevice(DeviceType.keyboard, "Keyboard"));
		
		loadControllers();
	}

	public static void setDebug(boolean debug){
		Inputs.debug = debug;
	}

	public static void useControllers(boolean use){
		useControllers = use;
		if(!use) {
            InputDevice keyboard = devices.get(0);
            devices.clear();
            devices.add(keyboard);
        }
	}

	protected static void loadControllers(){
		int i = 0;
		for(Controller c : Controllers.getControllers()){
			Inputs.getDevices().add(new InputDevice(DeviceType.controller, "Controller " + (++i), c));
		}

		Controllers.addListener(new ControllerAdapter(){
			@Override
			public void connected(Controller controller){
			    if(!useControllers) return;
				InputDevice device = new InputDevice(DeviceType.controller, "Controller " + Controllers.getControllers().size, controller);
				Inputs.getDevices().add(device);
			}

            @Override
            public void disconnected(Controller controller){
                for(InputDevice d : Inputs.getDevices()){
                    if(d.controller == controller){
                        Inputs.getDevices().removeValue(d, true);
                        for(Section s : KeyBinds.getSections()){
                            if(s.device == d){
                                s.device = devices.get(0);
                            }
                        }
                        break;
                    }
                }
            }

			@Override
			public boolean axisMoved(Controller controller, int axisIndex, float value) {
				if(Math.abs(value) > 0.3f && debug){
					Log.info("Axis: {0}, Code: {1}, Value: {2},", Input.findByType(Type.controller, axisIndex, true), axisIndex,  value);
				}
				return false;
			}

			@Override
			public boolean povMoved (Controller controller, int povIndex, PovDirection value) {
				if(debug){
					Log.info("POV: {0}, Code: {1}, Value: {2}", Input.findByType(Type.controller, povIndex, false), povIndex,  value);
				}
				return false;
			}

			@Override
			public boolean buttonDown(Controller controller, int buttonCode) {
				if(debug) Log.info("Button: {0}, Code: {1}", Input.findByType(Type.controller, buttonCode, false),  buttonCode);

				InputDevice device = findBy(controller);
				if(device == null) return false;
				device.pressed[buttonCode] = true;
				return false;
			}

			@Override
			public boolean buttonUp(Controller controller, int buttonCode) {
				InputDevice device = findBy(controller);
				if(device == null) return false;
				device.released[buttonCode] = true;
				return false;
			}
		});
	}

	private static InputDevice findBy(Controller controller){
		for(InputDevice d : devices){
			if(d.controller == controller)
				return d;
		}
		return null;
	}

	public static InputMultiplexer getProcessor(){
		return plex;
	}
	
	public static Array<InputDevice> getDevices(){
		return devices;
	}

	/**Call this at the end of each render loop.*/
	public static void update(){
		for(int i = 0; i < buttons.length; i ++){
			buttons[i] = Gdx.input.isButtonPressed(i);
		}
		scroll = 0;
		for(InputDevice device : devices){
			if(device.type == DeviceType.keyboard) continue;

			for(int i = 0; i < device.pressed.length; i ++){
				device.pressed[i] = false;
				device.released[i] = false;
				device.axes[i] = device.controller.getAxis(i);
			}

			device.lastPOV = device.controller.getPov(0);
		}
		keysReleased.clear();
	}
	
	public static void clearProcessors(){
		plex.getProcessors().clear();
	}
	
	/**Adds another input processor to the chain.*/
	public static void addProcessor(InputProcessor listener){
		plex.addProcessor(listener);
		Gdx.input.setInputProcessor(plex);
	}
	
	/**Adds another input processor to the chain at a specific index.*/
	public static void addProcessor(int index, InputProcessor listener){
		plex.addProcessor(index, listener);
		Gdx.input.setInputProcessor(plex);
	}
	
	public static void flipProcessors(){
		plex.getProcessors().reverse();
	}
/*
	public static boolean keyDown(int key){
		return Gdx.input.isKeyPressed(key);
	}
	
	public static boolean keyTap(int key){
		return Gdx.input.isKeyJustPressed(key);
	}

	public static boolean keyRelease(int key){
		return keysReleased.contains(key);
	}*/
	
	public static boolean keyDown(String name){
		return keyDown("default", name);
	}
	
	public static boolean keyTap(String name){
		return keyTap("default", name);
	}

	public static boolean keyRelease(String name){
		return keyRelease("default", name);
	}

	public static boolean keyDown(Input input){
		return keyDown(input, KeyBinds.getSection("default").device);
	}

	public static boolean keyDown(Input input, InputDevice device){
		if(input == Input.UNSET)
			return false;

		if(input.type == Input.Type.controller){
			if(input.axis) return device.controller.getAxis(input.code) > 0f;
			if(input.pov) return device.controller.getPov(input.code) == input.direction;
			return input.code >= 0 && device.controller.getButton(input.code);
		}else if(input.type == Input.Type.key){
			return input.code >= 0 && Gdx.input.isKeyPressed(input.code);
		}else if(input.type == Input.Type.mouse){
			return Gdx.input.isButtonPressed(input.code);
		}
		return false;
	}
	
	public static boolean keyDown(String section, String name){
		KeyBinds.Section s = KeyBinds.getSection(section);
		if(KeyBinds.has(section, name)){
			Input input = KeyBinds.get(section, s.device.type, name);
			return keyDown(input, s.device);
		}else{
			Input input = KeyBinds.get(section, DeviceType.keyboard, name);
			return keyDown(input, getKeyboard());
		}
	}

	public static boolean keyTap(Input input){
		return keyTap(input, KeyBinds.getSection("default").device);
	}

	public static boolean keyTap(Input input, InputDevice device){
		if(input == Input.UNSET)
			return false;

		if(input.type == Input.Type.controller){
            if(input.axis) return device.controller.getAxis(input.code) > 0f && device.axes[input.code] < 0;
			if(input.pov) return device.controller.getPov(input.code) == input.direction && device.lastPOV != input.direction;
			return input.code >= 0 && device.pressed[input.code];
		}else if(input.type == Input.Type.key){
			return Gdx.input.isKeyJustPressed(input.code);
		}else if(input.type == Input.Type.mouse){
			return Gdx.input.isButtonPressed(input.code) && !buttons[input.code];
		}
		return false;
	}
	
	public static boolean keyTap(String section, String name){
		KeyBinds.Section s = KeyBinds.getSection(section);
		if(KeyBinds.has(section, name)){
			Input input = KeyBinds.get(section, name);
			return keyTap(input, s.device);
		}else{
			Input input = KeyBinds.get(section, DeviceType.keyboard, name);
			return keyTap(input, getKeyboard());
		}
	}

	public static boolean keyRelease(Input input){
		return keyRelease(input, KeyBinds.getSection("default").device);
	}

	public static boolean keyRelease(Input input, InputDevice device){
		if(input == Input.UNSET)
			return false;

		if(input.type == Input.Type.controller){
			if(input.pov) return device.controller.getPov(input.code) != input.direction && device.lastPOV == input.direction;
			return input.code >= 0 && device.released[input.code];
		}else if(input.type == Input.Type.key){
			return keysReleased.contains(input.code);
		}else if(input.type == Input.Type.mouse){
			return !Gdx.input.isButtonPressed(input.code) && buttons[input.code];
		}
		return false;
	}

	public static boolean keyRelease(String section, String name){
		KeyBinds.Section s = KeyBinds.getSection(section);
		Input input = KeyBinds.get(section, name);
		if(KeyBinds.has(section, name)){
			return keyRelease(input, s.device);
		}else{
			return keyRelease(input, getKeyboard());
		}
	}

	public static boolean getAxisActive(String axis){
		return Math.abs(getAxis("default", axis)) > 0;
	}

	public static float getAxis(String axis){
		return getAxis("default", axis);
	}

	public static float getAxis(String section, String name){
		KeyBinds.Section s = KeyBinds.getSection(section);
		Axis axis = KeyBinds.getAxis(section, name);

		if(s.device.type == DeviceType.controller){
			Controller c = s.device.controller;

			if(axis.min.axis){
				return c.getAxis(axis.min.code) * (axis.min.name().contains("VERTICAL") && !OS.isWindows ? -1 : 1);
			}else{
				boolean min = axis.min.pov ? c.getPov(0) == axis.min.direction : c.getButton(axis.min.code),
						max = axis.max.pov ? c.getPov(0) == axis.max.direction : c.getButton(axis.max.code);
				return (min && max) || (!min && !max) ? 0 : min ? -1 : 1;
			}
		}else{
			if(axis.min == Input.SCROLL){
				return scroll();
			}else {
				boolean min = keyDown(axis.min, s.device), max = keyDown(axis.max, s.device);
				return (min && max) || (!min && !max) ? 0 : min ? -1 : 1;
			}
		}
	}

	public static InputDevice getKeyboard(){
		return devices.get(0);
	}
	
	public static boolean buttonDown(int button){
		return Gdx.input.isButtonPressed(button);
	}
	
	public static boolean buttonUp(int button){
		return Gdx.input.isButtonPressed(button) && !buttons[button];
	}
	
	public static boolean buttonRelease(int button){
		return !Gdx.input.isButtonPressed(button) && buttons[button];
	}
	
	public static int scroll(){
		return scroll;
	}
	
	public static boolean scrolled(){
		return Math.abs(scroll) > 0;
	}
	
	static void dispose(){
		plex.getProcessors().clear();
	}

	public static ControllerType getControllerType(Controller controller){
		String check = controller.getName().toLowerCase();
		if(check.contains("x-box") || check.contains("xbox")){
			return ControllerType.xbox;
		}else{
			return ControllerType.unknown;
		}
	}

	/**Represents either a keyboard or controller.*/
	public static class InputDevice{
		public static final int BUTTONS = 32;

		public final DeviceType type;
		public final String name;
		public final Controller controller;
		public final ControllerType controllerType;
		public final boolean[] pressed = new boolean[BUTTONS];
		public final boolean[] released = new boolean[BUTTONS];
		public final float[] axes = new float[BUTTONS];
		public PovDirection lastPOV = PovDirection.center;
		
		public InputDevice(DeviceType type, String name){
			this(type, name, null);
		}
		
		public InputDevice(DeviceType type, String name, Controller controller){
			this.type = type;
			this.name = name;
			this.controller = controller;
			if(controller != null){
				controllerType = getControllerType(controller);
			}else{
				controllerType = null;
			}
		}
	}

	//TODO 2D axes, like controller sticks, as having two different axes is confusing.
	//TODO don't treat triggers as axes?
	/**Represents an input axis. When using a mouse or keyboard, both values are used;
	 * When a controller is used, min is the only key checked, as it is already an axis.*/
	public static class Axis{
		public Input min, max;

		/**Cosntructor for axes only.*/
		public Axis(Input axis){
			min = max = axis;
		}

		/**Constructor for keyboards, or multiple buttons on a controller.*/
		public Axis(Input min, Input max){
			this.min = min;
			this.max = max;
		}
	}
	
	public enum DeviceType{
		keyboard, controller
	}

	public enum ControllerType{
		xbox, unknown;
	}
}
