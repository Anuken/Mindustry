package io.anuke.ucore.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.ucore.function.Listenable;

public class Settings{
	private static Preferences prefs;
	private static ObjectMap<String, Object> defaults = new ObjectMap<>();
	private static boolean disabled = false;
	private static Listenable errorHandler;
	
	public static void setErrorHandler(Listenable handler){
		errorHandler = handler;
	}
	
	public static void load(String name){
		prefs = Gdx.app.getPreferences(name);
	}
	
	/**Loads binds as well as prefs.*/
	public static void loadAll(String name){
		load(name);
		KeyBinds.load();
	}
	
	public static Object getDefault(String name){
		return defaults.get(name);
	}
	
	public static void put(String name, Object val){
		if(val instanceof Float)
			putFloat(name, (Float)val);
		else if(val instanceof Integer)
			putInt(name, (Integer)val);
		else if(val instanceof String)
			putString(name, (String)val);
		else if(val instanceof Boolean)
			putBool(name, (Boolean)val);
		else if(val instanceof Long)
			putLong(name, (Long)val);
	}
	
	public static void putString(String name, String val){
		prefs.putString(name, val);
	}
	
	public static void putFloat(String name, float val){
		prefs.putFloat(name, val);
	}
	
	public static void putInt(String name, int val){
		prefs.putInteger(name, val);
	}
	
	public static void putBool(String name, boolean val){
		prefs.putBoolean(name, val);
	}
	
	public static void putLong(String name, long val){
		prefs.putLong(name, val);
	}
	
	public static String getString(String name){
		return prefs.getString(name, (String)def(name));
	}
	
	public static float getFloat(String name){
		return prefs.getFloat(name, (Float)def(name));
	}
	
	public static int getInt(String name){
		return prefs.getInteger(name, (Integer)def(name));
	}
	
	public static boolean getBool(String name){
		return prefs.getBoolean(name, (Boolean)def(name));
	}
	
	public static long getLong(String name){
		return prefs.getLong(name, (Long)def(name));
	}


	public static String getString(String name, String def){
		return prefs.getString(name, def);
	}

	public static float getFloat(String name, float def){
		return prefs.getFloat(name, def);
	}

	public static int getInt(String name, int def){
		return prefs.getInteger(name, def);
	}

	public static boolean getBool(String name, boolean def){
		return prefs.getBoolean(name, def);
	}

	public static long getLong(String name, long def){
		return prefs.getLong(name, def);
	}
	
	public static boolean has(String name){
		return prefs.contains(name);
	}
	
	public static void save(){
		try{
			prefs.flush();
		}catch (GdxRuntimeException e){
			if(errorHandler != null){
				if(!disabled){
					errorHandler.listen();
				}
			}else{
				throw e;
			}
			
			disabled = true;
		}
	}
	
	public static Object def(String name){
		if(!defaults.containsKey(name))
			throw new IllegalArgumentException("No setting with name \"" + name + "\" exists!");
		return defaults.get(name);
	}
	
	/**Set up a bunch of defaults.
	 * Format: name1, default1, name2, default2, etc
	 */
	public static void defaultList(Object...objects){
		for(int i = 0; i < objects.length; i +=2 ){
			defaults((String)objects[i], objects[i+1]);
		}
	}
	
	/**Sets a default value up.
	*  This is REQUIRED for every pref value.*/
	public static void defaults(String name, Object object){
		defaults.put(name, object);
	}
}
