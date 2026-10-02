package arc.util.serialization;

import arc.files.*;
import arc.struct.*;
import arc.util.*;

import java.io.*;

/** An hsjon parser. Can be used as a standard json value.
 * Output can be converted to standard JSON. This class is heavily based upon the Hjson Java implementation.*/
public interface Jval{
    Jval
    trueValue = new JsonBool(true),
    falseValue = new JsonBool(false),
    nullValue = new JsonNull();

    static JsonMap newObject(){
        return new JsonMap();
    }

    static JsonArray newArray(){
        return new JsonArray();
    }

    /**
     * Reads a Hjson value from the given reader.
     * <p>
     * Characters are read in chunks and buffered internally, therefore wrapping an existing reader in
     * an additional <code>BufferedReader</code> does <strong>not</strong> improve reading
     * performance.
     * </p>
     * @param reader the reader to read the Hjson value from
     * @return the Hjson value that has been read
     */
    static Jval read(Reader reader){
        try{
            return new JvalReader(reader).parse();
        }catch(IOException e){
            throw new ArcRuntimeException(e);
        }
    }

    static Jval read(byte[] bytes){
        try{
            return new JvalReader(new InputStreamReader(new ByteArrayInputStream(bytes))).parse();
        }catch(IOException e){
            throw new ArcRuntimeException(e);
        }
    }

    /**
     * Reads a Hjson value from the given string.
     * @param text the string that contains the Hjson value
     * @return the Hjson value that has been read
     */
    static Jval read(String text){
        return new JvalReader(text).parse();
    }

    static Jval read(Fi file){
        return read(file.reader());
    }

    static Jval valueOf(int value){ return new JsonLong(value); }
    static Jval valueOf(long value){ return new JsonLong(value); }
    static Jval valueOf(float value){ return new JsonDouble(value); }
    static Jval valueOf(double value){ return new JsonDouble(value); }
    static Jval valueOf(String string){ return string == null ? nullValue : new JsonString(string); }
    static Jval valueOf(boolean value){ return value ? trueValue : falseValue; }
    /** Buckets any Number into a long or double value, depending on its kind. */
    static Jval valueOf(Number value){ return value instanceof Float || value instanceof Double ? valueOf(value.doubleValue()) : valueOf(value.longValue()); }

    Jtype getType();

    default boolean isObject(){ return getType() == Jtype.object; }
    default boolean isArray(){ return getType() == Jtype.array; }
    default boolean isNumber(){ return getType() == Jtype.number; }
    default boolean isString(){ return getType() == Jtype.string; }
    default boolean isBoolean(){ return getType() == Jtype.bool; }
    default boolean isTrue(){ return this == trueValue; }
    default boolean isFalse(){ return this == falseValue; }
    default boolean isNull(){ return getType() == Jtype.nil; }

    default JsonMap asObject(){ throw new UnsupportedOperationException("Not an object: " + this); }
    default JsonArray asArray(){ throw new UnsupportedOperationException("Not an array: " + this); }

    default byte asByte(){ throw new UnsupportedOperationException("Not a number: " + this); }
    default short asShort(){ throw new UnsupportedOperationException("Not a number: " + this); }
    default int asInt(){ throw new UnsupportedOperationException("Not a number: " + this); }
    default long asLong(){ throw new UnsupportedOperationException("Not a number: " + this); }
    default float asFloat(){ throw new UnsupportedOperationException("Not a number: " + this); }
    default double asDouble(){ throw new UnsupportedOperationException("Not a number: " + this); }
    default @Nullable String asString(){ throw new UnsupportedOperationException("Not a string: " + this); }
    default boolean asBool(){ throw new UnsupportedOperationException("Not a bool: " + this); }

    default byte[] asByteArray(){
        JsonArray arr = asArray();
        byte[] out = new byte[arr.size];
        for(int i = 0; i < out.length; i++) out[i] = arr.get(i).asByte();
        return out;
    }

    default short[] asShortArray(){
        JsonArray arr = asArray();
        short[] out = new short[arr.size];
        for(int i = 0; i < out.length; i++) out[i] = arr.get(i).asShort();
        return out;
    }

    default int[] asIntArray(){
        JsonArray arr = asArray();
        int[] out = new int[arr.size];
        for(int i = 0; i < out.length; i++) out[i] = arr.get(i).asInt();
        return out;
    }

    default long[] asLongArray(){
        JsonArray arr = asArray();
        long[] out = new long[arr.size];
        for(int i = 0; i < out.length; i++) out[i] = arr.get(i).asLong();
        return out;
    }

    default float[] asFloatArray(){
        JsonArray arr = asArray();
        float[] out = new float[arr.size];
        for(int i = 0; i < out.length; i++) out[i] = arr.get(i).asFloat();
        return out;
    }

    default double[] asDoubleArray(){
        JsonArray arr = asArray();
        double[] out = new double[arr.size];
        for(int i = 0; i < out.length; i++) out[i] = arr.get(i).asDouble();
        return out;
    }

    default boolean[] asBooleanArray(){
        JsonArray arr = asArray();
        boolean[] out = new boolean[arr.size];
        for(int i = 0; i < out.length; i++) out[i] = arr.get(i).asBool();
        return out;
    }

    default String[] asStringArray(){
        JsonArray arr = asArray();
        String[] out = new String[arr.size];
        for(int i = 0; i < out.length; i++) out[i] = arr.get(i).asString();
        return out;
    }

    default Jval get(int i){
        throw new UnsupportedOperationException("Not an array: " + this);
    }

    default Jval get(String name){
        throw new UnsupportedOperationException("Not an object: " + this);
    }

    default Jval add(Jval value){
        throw new UnsupportedOperationException("Not an array: " + this);
    }

    default Jval add(String value){
        return add(valueOf(value));
    }

    default Jval add(Number value){
        return add(valueOf(value));
    }

    default Jval add(boolean value){
        return add(valueOf(value));
    }

    default void add(String name, String val){
        add(name, valueOf(val));
    }

    default void add(String name, Jval val){
        throw new UnsupportedOperationException("Not an object: " + this);
    }

    default Jval put(String name, Jval val){
        if(val != null) add(name, val);
        return this;
    }

    default Jval put(String name, String val){
        if(val != null) add(name, val);
        return this;
    }

    default Jval put(String name, Number val){
        if(val != null) add(name, valueOf(val));
        return this;
    }

    default Jval put(String name, boolean val){
        add(name, valueOf(val));
        return this;
    }

    default Jval clear(){
        return this;
    }

    default Jval remove(String name){
        if(name == null) throw new NullPointerException("name is null");
        return asObject().removeKey(name);
    }

    default boolean has(String name){
        if(name == null) throw new NullPointerException("name is null");
        return asObject().containsKey(name);
    }

    default int getInt(String name){
        return getInt(name, 0);
    }

    default int getInt(String name, int defaultValue){
        Jval value = get(name);
        return value != null ? value.asInt() : defaultValue;
    }

    default long getLong(String name, long defaultValue){
        Jval value = get(name);
        return value != null ? value.asLong() : defaultValue;
    }

    default float getFloat(String name, float defaultValue){
        Jval value = get(name);
        return value != null ? value.asFloat() : defaultValue;
    }

    default double getDouble(String name, double defaultValue){
        Jval value = get(name);
        return value != null ? value.asDouble() : defaultValue;
    }

    default boolean getBool(String name, boolean defaultValue){
        Jval value = get(name);
        return value != null ? value.asBool() : defaultValue;
    }

    default long getLong(String name){
        return getLong(name, 0L);
    }

    default float getFloat(String name){
        return getFloat(name, 0f);
    }

    default double getDouble(String name){
        return getDouble(name, 0);
    }

    default boolean getBool(String name){
        return getBool(name, false);
    }

    default @Nullable String getString(String name){
        return getString(name, "");
    }

    default String getString(String name, String defaultValue){
        Jval value = get(name);
        return value != null && !value.isNull() ? value.asString() : defaultValue;
    }

    /**
     * Writes the JSON representation of this value to the given writer in its minimal form, without
     * any additional whitespace.
     * <p>
     * Writing performance can be improved by using a {@link java.io.BufferedWriter BufferedWriter}.
     * </p>
     * @param writer the writer to write this value to
     * @throws IOException if an I/O error occurs in the writer
     */
    default void writeTo(Writer writer) throws IOException{
        writeTo(writer, Jformat.plain);
    }

    /**
     * Writes the JSON/Hjson representation of this value to the given writer using the given formatting.
     * <p>
     * Writing performance can be improved by using a {@link java.io.BufferedWriter BufferedWriter}.
     * </p>
     * @param writer the writer to write this value to
     * @param format controls the formatting
     * @throws IOException if an I/O error occurs in the writer
     */
    default void writeTo(Writer writer, Jformat format) throws IOException{
        WritingBuffer buffer = new WritingBuffer(writer, 128);
        switch(format){
            case plain:
                JvalWriter.writeJson(this, false, true, buffer, 0);
                break;
            case minimal:
                JvalWriter.writeJson(this, false, false, buffer, 0);
                break;
            case formatted:
                JvalWriter.writeJson(this, true, true, buffer, 0);
                break;
            case hjson:
                JvalWriter.writeHjson(this, buffer, -1, "", true);
                break;
        }
        buffer.flush();
    }

    /**
     * Returns the JSON/Hjson string for this value using the given formatting.
     * @param format controls the formatting
     * @return a JSON/Hjson string that represents this value
     */
    default String toString(Jformat format){
        StringWriter writer = new StringWriter();
        try{
            writeTo(writer, format);
        }catch(IOException exception){
            // StringWriter does not throw IOExceptions
            throw new RuntimeException(exception);
        }
        return writer.toString();
    }

    /** Holds a true/false value; true and false are the shared singleton instances. */
    class JsonBool implements Jval{
        private final boolean value;

        JsonBool(boolean value){ this.value = value; }

        @Override public Jtype getType(){ return Jtype.bool; }
        @Override public boolean asBool(){ return value; }
        @Override public String toString(){ return value ? "true" : "false"; }
        @Override public boolean equals(Object o){ return o instanceof JsonBool && ((JsonBool)o).value == value; }
        @Override public int hashCode(){ return Boolean.hashCode(value); }
    }

    /** Holds an integral number as a raw, unboxed long. */
    class JsonLong implements Jval{
        private final long value;

        JsonLong(long value){ this.value = value; }

        @Override public Jtype getType(){ return Jtype.number; }
        @Override public byte asByte(){ return (byte)value; }
        @Override public short asShort(){ return (short)value; }
        @Override public int asInt(){ return (int)value; }
        @Override public long asLong(){ return value; }
        @Override public float asFloat(){ return value; }
        @Override public double asDouble(){ return value; }
        @Override public String asString(){ return String.valueOf(value); }
        @Override public String toString(){ return Long.toString(value); }
        @Override public boolean equals(Object o){ return o instanceof JsonLong && ((JsonLong)o).value == value; }
        @Override public int hashCode(){ return Long.hashCode(value); }
    }

    /** Holds a floating-point number as a raw, unboxed double. */
    class JsonDouble implements Jval{
        private final double value;

        JsonDouble(double value){ this.value = value; }

        @Override public Jtype getType(){ return Jtype.number; }
        @Override public byte asByte(){ return (byte)value; }
        @Override public short asShort(){ return (short)value; }
        @Override public int asInt(){ return (int)value; }
        @Override public long asLong(){ return (long)value; }
        @Override public float asFloat(){ return (float)value; }
        @Override public double asDouble(){ return value; }
        @Override public String asString(){ return String.valueOf(value); }

        @Override
        public String toString(){
            String s = Double.toString(value);
            if(s.endsWith(".0")) s = s.substring(0, s.length() - 2);
            return s.replace('E', 'e');
        }

        @Override public boolean equals(Object o){ return o instanceof JsonDouble && ((JsonDouble)o).value == value; }
        @Override public int hashCode(){ return Double.hashCode(value); }
    }

    /** Holds a string value. */
    class JsonString implements Jval{
        private final String value;

        JsonString(String value){ this.value = value; }

        @Override public Jtype getType(){ return Jtype.string; }
        @Override public String asString(){ return value; }
        @Override public byte asByte(){ return Byte.parseByte(value); }
        @Override public short asShort(){ return Short.parseShort(value); }
        @Override public int asInt(){ return Integer.parseInt(value); }
        @Override public long asLong(){ return Long.parseLong(value); }
        @Override public float asFloat(){ return Float.parseFloat(value); }
        @Override public double asDouble(){ return Double.parseDouble(value); }
        @Override public boolean asBool(){ return Boolean.parseBoolean(value); }
        @Override public String toString(){ return value; }
        @Override public boolean equals(Object o){ return o instanceof JsonString && ((JsonString)o).value.equals(value); }
        @Override public int hashCode(){ return value.hashCode(); }
    }

    /** Holds no value at all; represents JSON null. */
    class JsonNull implements Jval{
        JsonNull(){}

        @Override public Jtype getType(){ return Jtype.nil; }
        @Override public @Nullable String asString(){ return null; }
        @Override public String toString(){ return "null"; }
        @Override public boolean equals(Object o){ return o instanceof JsonNull; }
        @Override public int hashCode(){ return 0; }
    }

    /** Alias class of whatever is used to store json maps (objects). */
    class JsonMap extends ArrayMap<String, Jval> implements Jval{

        public JsonMap(){
            //maps have no capacity until actually used; this calls resize() after the first put(), but that's fine, there is no initial allocation with 0 capacity
            super(0);
        }

        @Override public Jtype getType(){ return Jtype.object; }
        @Override public JsonMap asObject(){ return this; }
        @Override public String toString(){ return toString(Jformat.minimal); }

        @Override
        public JsonMap put(String key, Jval value){
            super.put(key, value);
            return this;
        }

        @Override
        public JsonMap clear(){
            super.clear();
            return this;
        }

        @Override
        public void add(String name, Jval val){
            if(name == null) throw new NullPointerException("name is null");
            put(name, val == null ? nullValue : val);
        }

        /** Puts a value without checking if it's in the map first. This is unsafe, but prevents O(n) put. */
        public void putAdd(String key, Jval value){
            if(size == ((Object[])keys).length) resize(Math.max(8, (int)(size * 1.75f)));
            int index = size++;
            ((Object[])keys)[index] = key;
            ((Object[])values)[index] = value;
        }
    }

    /** Alias class of json arrays. */
    class JsonArray extends Seq<Jval> implements Jval{

        public JsonArray(){
            super(false, 8);
        }

        @Override
        public JsonArray add(Jval value){
            super.add(value);
            return this;
        }

        @Override
        public JsonArray clear(){
            super.clear();
            return this;
        }

        @Override public Jtype getType(){ return Jtype.array; }
        @Override public JsonArray asArray(){ return this; }
        @Override public String toString(){ return toString(Jformat.minimal); }
    }

    /** The toString format. */
    enum Jformat{
        /** JSON (no whitespace). */
        plain,
        /** Minimal quote-less JSON. Equivalent to libGDX's minimal output type. */
        minimal,
        /** Formatted JSON. */
        formatted,
        /** Formatted HJSON. */
        hjson,
    }

    /** Defines the known json types. */
    enum Jtype{
        string, number, object, array, bool, nil,
    }

}