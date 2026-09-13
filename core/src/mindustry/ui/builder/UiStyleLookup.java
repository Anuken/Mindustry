package mindustry.ui.builder;

import arc.struct.*;
import arc.util.*;
import mindustry.ui.*;

/** Grabs style fields by name from {@link Styles}. */
public class UiStyleLookup{
    private static final ObjectMap<Class<?>, ObjectMap<String, Object>> styles = new ObjectMap<>();

    public static <T> @Nullable T get(Class<T> type, String name){
        if(styles.isEmpty()) init();
        return (T)styles.get(type, ObjectMap::new).get(name);
    }

    private static void init(){
        for(var field : Styles.class.getFields()){
            try{
                styles.get(field.getType(), ObjectMap::new).put(field.getName(), field.get(null));
            }catch(Exception e){
                Log.err(e);
            }
        }
    }
}
