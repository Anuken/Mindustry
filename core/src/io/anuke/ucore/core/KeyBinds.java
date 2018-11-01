package io.anuke.ucore.core;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import com.badlogic.gdx.utils.OrderedMap;
import io.anuke.ucore.core.Inputs.Axis;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.Inputs.InputDevice;
import io.anuke.ucore.util.Input;
import io.anuke.ucore.util.Mathf;

public class KeyBinds{
	private static OrderedMap<String, Section> map = new OrderedMap<>();

    /**Format:
     * name, keybind, name2, keybind2...
	 * Make sure you define a default for each key you use!*/
	public static void defaults(Object... keys){
        defaultSection("default", DeviceType.keyboard, keys);
    }

    public static void defaults(DeviceType type, Object... keys){
        defaultSection("default", type, keys);
    }

    public static void defaultSection(String sectionName, DeviceType type, Object... keys){
		if(!map.containsKey(sectionName)){
			map.put(sectionName, new Section());
		}
        Section section = map.get(sectionName);

		for(DeviceType other : DeviceType.values()){
			if(!section.defaults.containsKey(other)) {
				section.defaults.put(other, new OrderedMap<>());
				section.binds.put(other, new OrderedMap<>());
			}
			if(!section.axisDefaults.containsKey(other)) {
				section.axisDefaults.put(other, new OrderedMap<>());
				section.axisBinds.put(other, new OrderedMap<>());
			}
			if(!section.keybinds.containsKey(other)){
				section.keybinds.put(other, new Array<>());
			}
		}

        for(int i = 0; i < keys.length/2; i ++){
            if(!(keys[i*2] instanceof String)) throw new IllegalArgumentException("Invalid keybind format!");
            String key = (String)keys[i*2];
            Object to = keys[i*2+1];

            if(to instanceof Axis){
                section.axisDefaults.get(type).put(key, (Axis)to);
				section.keybinds.get(type).add(new Keybind(key, (Axis)to));
            }else if(to instanceof Input){
                section.defaults.get(type).put(key, (Input)to);
				section.keybinds.get(type).add(new Keybind(key, (Input)to));
            }else{
                throw new IllegalArgumentException("Invalid keybind format!");
            }
        }
	}

	public static void save(){
		for(Section sec : map.values()){
			for(DeviceType type : DeviceType.values()){
				for(String name : sec.binds.get(type).keys()){
					String text = "keybind-" + sec.name + "-" + type.name() + "-" + name;
					Input input = sec.binds.get(type).get(name);
					Settings.putInt(text, input.ordinal());
				}

				for(String name : sec.axisBinds.get(type).keys()){
					String text = "axis-" + sec.name + "-" + type.name() + "-" + name;
					Inputs.Axis axis = sec.axisBinds.get(type).get(name);
					Settings.putInt(text + "-min", axis.min.ordinal());
					Settings.putInt(text + "-max", axis.max.ordinal());
				}
			}
			Settings.putInt(sec.name + "-last-device-type", Inputs.getDevices().indexOf(map.get("default").device, true));
		}

		Settings.save();
	}

	public static void load(){
		Input[] values = Input.values();
		for(Section sec : map.values()){
		    for(DeviceType type : DeviceType.values()){
                for(String name : sec.defaults.get(type).keys()){
                	int key = Settings.getInt("keybind-" + sec.name + "-" + type.name() + "-" + name, sec.defaults.get(type).get(name).ordinal());
                	Input input = key == -1 ? Input.UNSET : values[key];
                	sec.binds.get(type).put(name, input);
                }

				for(String name : sec.axisDefaults.get(type).keys()){
					String text = "axis-" + sec.name + "-" + type.name() + "-" + name;
					Axis def = sec.axisDefaults.get(type).get(name);
					int mi = Settings.getInt(text + "-min", def.min.ordinal());
					int ma = Settings.getInt(text + "-max", def.max.ordinal());
					Input min = mi == -1 ? Input.UNSET : values[mi];
					Input max = ma == -1 ? Input.UNSET : values[ma];

					Inputs.Axis axis = sec.axisBinds.get(type).get(name);
					if(axis == null) sec.axisBinds.get(type).put(name, axis = new Inputs.Axis(min, max));

					axis.min = min;
					axis.max = max;
				}
            }
            sec.device = Inputs.getDevices().get(Mathf.clamp(Settings.getInt(sec.name + "-last-device-type", 0), 0, Inputs.getDevices().size-1));
		}
	}


	public static void resetToDefaults(){
		Input[] values = Input.values();
		for(Section sec : map.values()){
			for(DeviceType type : DeviceType.values()){
				for(String name : sec.defaults.get(type).keys()){
					sec.binds.get(type).put(name, sec.defaults.get(type).get(name));
				}

				for(String name : sec.axisDefaults.get(type).keys()){
					Axis axis = sec.axisBinds.get(type).get(name);
					Axis def = sec.axisDefaults.get(type).get(name);
					axis.min = def.min;
					axis.max = def.max;
				}
			}
		}
	}

	public static Array<Section> getSections(){
	    return map.values().toArray();
    }

    public static Section getSection(String name){
	    return map.get(name);
    }

	public static Input get(String section, String name){
		Section s = map.get(section);
		if(s == null)
			throw new IllegalArgumentException("No section \"" + section + "\" found!");
	    return get(section, s.device.type, name);
	}

	public static Input get(String section, DeviceType type, String name){
		Section s = map.get(section);
		if(s == null)
			throw new IllegalArgumentException("No section \"" + section + "\" found!");
		if(!s.defaults.get(type).containsKey(name))
			throw new IllegalArgumentException("No keybind \"" + name + "\" found in section \"" + section + "\"");

		return s.binds.get(type).get(name, s.defaults.get(type).get(name));
	}

	public static boolean has(String section, String name){
		Section s = map.get(section);
		if(s == null)
			throw new IllegalArgumentException("No section \"" + section + "\" found!");
		return s.defaults.get(s.device.type).containsKey(name);
	}

	public static Inputs.Axis getAxis(String section, String name){
		Section s = map.get(section);
		if(s == null)
			throw new IllegalArgumentException("No section \"" + section + "\" found!");
		if(!s.axisDefaults.get(s.device.type).containsKey(name))
			throw new IllegalArgumentException("No axis \"" + name + "\" found in section \"" + section + "\"");

		return s.axisBinds.get(s.device.type).get(name, s.axisDefaults.get(s.device.type).get(name));
	}

	public static Input get(String name){
		return get("default", name);
	}

	public static Inputs.Axis getAxis(String name){
		return getAxis("default", name);
	}

	/**A section represents a set of input binds, like controls for a specific player.
	 * Each section has a device, which may be a controller or keyboard, and a name (for example, "player2")
	 * The default section uses a keyboard.*/
	public static class Section{
		public ObjectMap<DeviceType, Array<Keybind>> keybinds = new OrderedMap<>();
		public ObjectMap<DeviceType, ObjectMap<String, Input>> binds = new ObjectMap<>();
		public ObjectMap<DeviceType, ObjectMap<String, Input>> defaults = new ObjectMap<>();
		public ObjectMap<DeviceType, ObjectMap<String, Inputs.Axis>> axisDefaults = new ObjectMap<>();
		public ObjectMap<DeviceType, ObjectMap<String, Inputs.Axis>> axisBinds = new ObjectMap<>();
		public InputDevice device = Inputs.getDevices().first();
		public String name;
	}

	/**Variant class that is either an axis or input key.*/
	public static class Keybind{
		public final Axis axis;
		public final Input input;
		public final String name;

		public Keybind(String name, Axis axis){
			this.axis = axis;
			this.input = null;
			this.name = name;
		}

		public Keybind(String name, Input input){
			this.axis = null;
			this.input = input;
			this.name = name;
		}

		public boolean isAxis(){
			return axis != null;
		}
	}
}
