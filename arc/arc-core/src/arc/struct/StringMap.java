package arc.struct;

import arc.util.Strings;

/** An ObjectMap with string keys and values. Comes with extra parsing utilities.*/
public class StringMap extends ObjectMap<String, String>{

    public static StringMap of(Object... values){
        StringMap map = new StringMap();

        for(int i = 0; i < values.length / 2; i++){
            map.put((String)values[i * 2], String.valueOf(values[i * 2 + 1]));
        }

        return map;
    }

    public StringMap(){

    }

    public StringMap(ObjectMap<? extends String, ? extends String> map){
        super(map);
    }

    public boolean getBool(String name){
        return get(name, "").equals("true");
    }

    public int getInt(String name){
        return getInt(name, 0);
    }

    public float getFloat(String name){
        return getFloat(name, 0f);
    }

    public long getLong(String name){
        return getLong(name, 0L);
    }

    public int getInt(String name, int def){
        return Strings.parseInt(get(name, ""), def);
    }

    public float getFloat(String name, float def){
        return Strings.parseFloat(get(name, ""), def);
    }

    public long getLong(String name, long def){
        return Strings.parseLong(get(name, ""), def);
    }
}
