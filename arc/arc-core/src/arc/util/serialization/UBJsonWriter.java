package arc.util.serialization;

import arc.struct.*;
import arc.util.*;

import java.io.*;

/**
 * Builder style API for emitting UBJSON.
 * @author Justin Shapcott
 */
public class UBJsonWriter implements JsonWriter{
    final DataOutputStream out;
    private final Seq<JsonObject> stack = new Seq<>();
    private JsonObject current;
    private boolean named;

    public UBJsonWriter(OutputStream out){
        if(!(out instanceof DataOutputStream)) out = new DataOutputStream(out);
        this.out = (DataOutputStream)out;
    }

    public void reset(){
        stack.clear();
        current = null;
        named = false;
    }

    /**
     * Begins a new object container. To finish the object call {@link #pop()}.
     * @return This writer, for chaining
     */
    @Override
    public UBJsonWriter object(){
        try{
            if(current != null){
                if(!current.array){
                    if(!named) throw new IllegalStateException("Name must be set.");
                    named = false;
                }
            }
            stack.add(current = new JsonObject(false));
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Begins a new named object container, having the given name. To finish the object call {@link #pop()}.
     * @return This writer, for chaining
     */
    @Override
    public UBJsonWriter object(String name){
        name(name).object();
        return this;
    }

    /**
     * Begins a new array container. To finish the array call {@link #pop()}.
     * @return this writer, for chaining.
     */
    @Override
    public UBJsonWriter array(){
        try{
            if(current != null){
                if(!current.array){
                    if(!named) throw new IllegalStateException("Name must be set.");
                    named = false;
                }
            }
            stack.add(current = new JsonObject(true));
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Begins a new named array container, having the given name. To finish the array call {@link #pop()}.
     * @return this writer, for chaining.
     */
    @Override
    public UBJsonWriter array(String name){
        name(name).array();
        return this;
    }

    /**
     * Appends a name for the next object, array, or value.
     * @return this writer, for chaining
     */
    @Override
    public UBJsonWriter name(String name){
        try{
            if(current == null || current.array) throw new IllegalStateException("Current item must be an object.");
            byte[] bytes = name.getBytes(Strings.utf8);
            if(bytes.length <= Byte.MAX_VALUE){
                out.writeByte('i');
                out.writeByte(bytes.length);
            }else if(bytes.length <= Short.MAX_VALUE){
                out.writeByte('I');
                out.writeShort(bytes.length);
            }else{
                out.writeByte('l');
                out.writeInt(bytes.length);
            }
            out.write(bytes);
            named = true;
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code byte} value to the stream. This corresponds to the {@code int8} value type in the UBJSON specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(byte value){
        try{
            checkName();
            out.writeByte('i');
            out.writeByte(value);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code short} value to the stream. This corresponds to the {@code int16} value type in the UBJSON specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(short value){
        try{
            checkName();
            out.writeByte('I');
            out.writeShort(value);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an {@code int} value to the stream. This corresponds to the {@code int32} value type in the UBJSON specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(int value){
        try{
            checkName();
            out.writeByte('l');
            out.writeInt(value);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code long} value to the stream. This corresponds to the {@code int64} value type in the UBJSON specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(long value){
        try{
            checkName();
            out.writeByte('L');
            out.writeLong(value);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code float} value to the stream. This corresponds to the {@code float32} value type in the UBJSON specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(float value){
        try{
            checkName();
            out.writeByte('d');
            out.writeFloat(value);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code double} value to the stream. This corresponds to the {@code float64} value type in the UBJSON
     * specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(double value){
        try{
            checkName();
            out.writeByte('D');
            out.writeDouble(value);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code boolean} value to the stream. This corresponds to the {@code boolean} value type in the UBJSON
     * specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(boolean value){
        try{
            checkName();
            out.writeByte(value ? 'T' : 'F');
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code char} value to the stream. Because, in Java, a {@code char} is 16 bytes, this corresponds to the
     * {@code int16} value type in the UBJSON specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(char value){
        try{
            checkName();
            out.writeByte('I');
            out.writeChar(value);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code String} value to the stream. This corresponds to the {@code string} value type in the UBJSON specification.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(String value){
        try{
            checkName();
            byte[] bytes = value.getBytes(Strings.utf8);
            out.writeByte('S');
            if(bytes.length <= Byte.MAX_VALUE){
                out.writeByte('i');
                out.writeByte(bytes.length);
            }else if(bytes.length <= Short.MAX_VALUE){
                out.writeByte('I');
                out.writeShort(bytes.length);
            }else{
                out.writeByte('l');
                out.writeInt(bytes.length);
            }
            out.write(bytes);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an optimized {@code byte array} value to the stream. As an optimized array, the {@code int8} value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(byte[] values){
        try{
            array();
            out.writeByte('$');
            out.writeByte('i');
            out.writeByte('#');
            value(values.length);
            for(int i = 0, n = values.length; i < n; i++){
                out.writeByte(values[i]);
            }
            pop(true);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an optimized {@code short array} value to the stream. As an optimized array, the {@code int16} value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(short[] values){
        try{
            array();
            out.writeByte('$');
            out.writeByte('I');
            out.writeByte('#');
            value(values.length);
            for(int i = 0, n = values.length; i < n; i++){
                out.writeShort(values[i]);
            }
            pop(true);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an optimized {@code int array} value to the stream. As an optimized array, the {@code int32} value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(int[] values){
        try{
            array();
            out.writeByte('$');
            out.writeByte('l');
            out.writeByte('#');
            value(values.length);
            for(int i = 0, n = values.length; i < n; i++){
                out.writeInt(values[i]);
            }
            pop(true);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an optimized {@code long array} value to the stream. As an optimized array, the {@code int64} value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(long[] values){
        try{
            array();
            out.writeByte('$');
            out.writeByte('L');
            out.writeByte('#');
            value(values.length);
            for(int i = 0, n = values.length; i < n; i++){
                out.writeLong(values[i]);
            }
            pop(true);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an optimized {@code float array} value to the stream. As an optimized array, the {@code float32} value type marker
     * and element count are encoded once at the array marker instead of repeating the type marker for each element.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(float[] values){
        try{
            array();
            out.writeByte('$');
            out.writeByte('d');
            out.writeByte('#');
            value(values.length);
            for(int i = 0, n = values.length; i < n; i++){
                out.writeFloat(values[i]);
            }
            pop(true);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an optimized {@code double array} value to the stream. As an optimized array, the {@code float64} value type marker
     * and element count are encoded once at the array marker instead of repeating the type marker for each element. element count
     * are encoded once at the array marker instead of for each element.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(double[] values){
        try{
            array();
            out.writeByte('$');
            out.writeByte('D');
            out.writeByte('#');
            value(values.length);
            for(int i = 0, n = values.length; i < n; i++){
                out.writeDouble(values[i]);
            }
            pop(true);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a {@code boolean array} value to the stream.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(boolean[] values){
        try{
            array();
            for(int i = 0, n = values.length; i < n; i++){
                out.writeByte(values[i] ? 'T' : 'F');
            }
            pop();
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an optimized {@code char array} value to the stream. As an optimized array, the {@code int16} value type marker and
     * element count are encoded once at the array marker instead of repeating the type marker for each element.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(char[] values){
        try{
            array();
            out.writeByte('$');
            out.writeByte('C');
            out.writeByte('#');
            value(values.length);
            for(int i = 0, n = values.length; i < n; i++){
                out.writeChar(values[i]);
            }
            pop(true);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends an optimized {@code String array} value to the stream. As an optimized array, the {@code String} value type marker
     * and element count are encoded once at the array marker instead of repeating the type marker for each element.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(String[] values){
        try{
            array();
            out.writeByte('$');
            out.writeByte('S');
            out.writeByte('#');
            value(values.length);
            for(int i = 0, n = values.length; i < n; i++){
                byte[] bytes = values[i].getBytes(Strings.utf8);
                if(bytes.length <= Byte.MAX_VALUE){
                    out.writeByte('i');
                    out.writeByte(bytes.length);
                }else if(bytes.length <= Short.MAX_VALUE){
                    out.writeByte('I');
                    out.writeShort(bytes.length);
                }else{
                    out.writeByte('l');
                    out.writeInt(bytes.length);
                }
                out.write(bytes);
            }
            pop(true);
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends the object to the stream, if it is a known value type. This is a convenience method that calls through to the
     * appropriate value method.
     * @return this writer, for chaining
     */
    @Override
    public UBJsonWriter value(Object object){
        if(object == null){
            return value();
        }else if(object instanceof Number){
            Number number = (Number)object;
            if(object instanceof Byte) return value(number.byteValue());
            if(object instanceof Short) return value(number.shortValue());
            if(object instanceof Integer) return value(number.intValue());
            if(object instanceof Long) return value(number.longValue());
            if(object instanceof Float) return value(number.floatValue());
            if(object instanceof Double) return value(number.doubleValue());
        }else if(object instanceof Character){
            return value(((Character)object).charValue());
        }else if(object instanceof CharSequence){
            return value(object.toString());
        }else if(object instanceof Boolean){
            return value((boolean)object);
        }else
            throw new SerializationException("Unknown object type.");

        return this;
    }

    /**
     * Appends a {@code null} value to the stream.
     * @return this writer, for chaining
     */
    public UBJsonWriter value(){
        try{
            checkName();
            out.writeByte('Z');
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /**
     * Appends a named value to the stream.
     * @return this writer, for chaining
     */
    @Override
    public UBJsonWriter set(String name, Object value){
        return name(name).value(value);
    }

    private void checkName(){
        if(current != null){
            if(!current.array){
                if(!named) throw new IllegalStateException("Name must be set.");
                named = false;
            }
        }
    }

    /**
     * Ends the current object or array and pops it off of the element stack.
     * @return This writer, for chaining
     */
    @Override
    public UBJsonWriter pop(){
        return pop(false);
    }

    protected UBJsonWriter pop(boolean silent){
        try{
            if(named) throw new IllegalStateException("Expected an object, array, or value since a name was set.");
            if(silent)
                stack.pop();
            else
                stack.pop().close();
            current = stack.size == 0 ? null : stack.peek();
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /** Flushes the underlying stream. This forces any buffered output bytes to be written out to the stream. */
    public void flush(){
        try{
            out.flush();
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    /** Closes the underlying output stream and releases any system resources associated with the stream. */
    @Override
    public void close(){
        try{
            while(stack.size > 0)
                pop();
            out.close();
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    private class JsonObject{
        final boolean array;

        JsonObject(boolean array) throws IOException{
            this.array = array;
            out.writeByte(array ? '[' : '{');
        }

        void close() throws IOException{
            out.writeByte(array ? ']' : '}');
        }
    }

}