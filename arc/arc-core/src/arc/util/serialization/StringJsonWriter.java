package arc.util.serialization;

import arc.struct.*;
import arc.util.serialization.Jval.*;

import java.io.*;

/** Builder-style JSON writer: writes directly to a stream instead of building an in-memory tree (unlike {@link Jval}). */
public class StringJsonWriter extends Writer implements JsonWriter{
    final Writer writer;
    private final Seq<JsonObject> stack = new Seq<>();
    private JsonObject current;
    private boolean named;
    private Jformat format = Jformat.minimal;

    public StringJsonWriter(Writer writer, Jformat format){
        this.writer = writer;
        this.format = format;
    }

    public StringJsonWriter(Writer writer){
        this.writer = writer;
    }

    @Override
    public JsonWriter name(String name){
        try{
            if(current == null || current.array) throw new IllegalStateException("Current item must be an object.");
            if(!current.needsComma)
                current.needsComma = true;
            else
                writer.write(',');
            newline(current.level + 1);
            writer.write(quoteName(name));
            writer.write(':');
            if(format == Jformat.formatted || format == Jformat.hjson) writer.write(' ');
            named = true;
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    @Override
    public JsonWriter object(){
        try{
            requireCommaOrName();
            int level = current == null ? 0 : current.level + 1;
            writer.write('{');
            stack.add(current = new JsonObject(false, level));
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    @Override
    public JsonWriter array(){
        try{
            requireCommaOrName();
            int level = current == null ? 0 : current.level + 1;
            writer.write('[');
            stack.add(current = new JsonObject(true, level));
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    @Override
    public JsonWriter value(Object value){
        try{
            requireCommaOrName();
            writer.write(quoteValue(value));
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    private void requireCommaOrName() throws IOException{
        if(current == null) return;
        if(current.array){
            if(!current.needsComma)
                current.needsComma = true;
            else
                writer.write(',');
            newline(current.level + 1);
        }else{
            if(!named) throw new IllegalStateException("Name must be set.");
            named = false;
        }
    }

    private void newline(int level) throws IOException{
        if(format != Jformat.formatted && format != Jformat.hjson) return;
        writer.write('\n');
        for(int i = 0; i < level; i++) writer.write("  ");
    }

    @Override
    public JsonWriter object(String name){
        return name(name).object();
    }

    @Override
    public JsonWriter array(String name){
        return name(name).array();
    }

    @Override
    public JsonWriter set(String name, Object value){
        return name(name).value(value);
    }

    @Override
    public JsonWriter pop(){
        try{
            if(named) throw new IllegalStateException("Expected an object, array, or value since a name was set.");
            JsonObject pop = stack.pop();
            current = stack.size == 0 ? null : stack.peek();
            if(pop.needsComma) newline(pop.level);
            writer.write(pop.array ? ']' : '}');
            return this;
        }catch(IOException ex){
            throw new SerializationException(ex);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException{
        writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException{
        writer.flush();
    }

    @Override
    public void close() throws IOException{
        while(stack.size > 0)
            pop();
        writer.close();
    }

    private String quoteName(String name){
        switch(format){
            case minimal:
            case hjson:
                return JvalWriter.escapeName(name);
            default:
                return '"' + JvalWriter.escapeString(name) + '"';
        }
    }

    private String quoteValue(Object value){
        if(value == null) return "null";
        if(value instanceof Number){
            Number number = (Number)value;
            long longValue = number.longValue();
            if(number.doubleValue() == longValue) return Long.toString(longValue);
            return number.toString();
        }
        if(value instanceof Boolean) return value.toString();

        String string = value.toString();
        switch(format){
            case minimal:
            case hjson:
                return JvalWriter.escapeName(string);
            default:
                return '"' + JvalWriter.escapeString(string) + '"';
        }
    }

    private static class JsonObject{
        final boolean array;
        final int level;
        boolean needsComma;

        JsonObject(boolean array, int level){
            this.array = array;
            this.level = level;
        }
    }
}