package arc.util.serialization;

import java.io.*;

public interface JsonWriter extends Closeable{
    JsonWriter name(String name);

    JsonWriter object();

    JsonWriter array();

    JsonWriter value(Object value);

    JsonWriter object(String name);

    JsonWriter array(String name);

    JsonWriter set(String name, Object value);

    JsonWriter pop();

    /** Starts a new object. Equivalent to {@link #object()}; provided for readability at call sites. */
    default JsonWriter writeObjectStart(){
        return object();
    }

    /** Starts a new named object. Equivalent to {@link #object(String)}. */
    default JsonWriter writeObjectStart(String name){
        return object(name);
    }

    /** Ends the current object. Equivalent to {@link #pop()}. */
    default JsonWriter writeObjectEnd(){
        return pop();
    }

    /** Starts a new array. Equivalent to {@link #array()}. */
    default JsonWriter writeArrayStart(){
        return array();
    }

    /** Starts a new named array. Equivalent to {@link #array(String)}. */
    default JsonWriter writeArrayStart(String name){
        return array(name);
    }

    /** Ends the current array. Equivalent to {@link #pop()}. */
    default JsonWriter writeArrayEnd(){
        return pop();
    }
}