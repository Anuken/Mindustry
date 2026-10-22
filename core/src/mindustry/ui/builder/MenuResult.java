package mindustry.ui.builder;

import arc.struct.*;
import arc.util.*;

public class MenuResult{
    /** Maximum length of result string */
    public static final int maxResultLen = 500;
    /** Maximum length of a single value in chars. */
    public static final int maxValueLen = 1000;
    /** Maximum total length of all string values in chars. This includes IDs of result values. */
    public static final int maxTotalStringLen = 6000;
    /** Maximum values count of any type that can be sent. */
    public static final int maxTotalValues = 500;

    /** Token passed to MenuBuilder. */
    public long token;
    /** Button that was actually clicked, or null if the dialog was closed (back button). */
    public @Nullable String result = null;
    /** Maps ID to element value (slider: value, text field: text, checkbox: checked) */
    public ObjectMap<String, Object> values = new ObjectMap<>();

    /** Implies cancelled results. */
    public MenuResult(){
    }

    public MenuResult(@Nullable String result){
        this.result = result;
    }

    /** @return if the dialog was closed with no choice being made. */
    public boolean wasCancelled(){
        return result == null;
    }

    /** @return whether the result ID matches the specified parameter. */
    public boolean is(String text){
        return Structs.eq(text, result);
    }

    public String getString(String id, String defaultValue){
        return values.get(id) instanceof String s ? s : defaultValue;
    }

    public @Nullable String getString(String id){
        return values.get(id) instanceof String s ? s : null;
    }

    public float getFloat(String id, float def){
        return values.get(id) instanceof Float s ? s : def;
    }

    public float getFloat(String id){
        return getFloat(id, 0f);
    }

    public boolean getBool(String id, boolean def){
        return values.get(id) instanceof Boolean s ? s : def;
    }

    public boolean getBool(String id){
        return getBool(id, false);
    }

    @Override
    public String toString(){
        return "MenuResult{" +
        "token=" + token +
        ", result='" + result + '\'' +
        ", values=" + values +
        '}';
    }
}