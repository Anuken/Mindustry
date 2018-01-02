package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Streamable {
    public transient ByteArrayInputStream stream;

    /**Marks the beginning of a stream.*/
    public static class StreamBegin{
        private static int lastid;

        public int id = lastid ++;
        public int total;
        public Class<? extends Streamable> type;
    }

    public static class StreamChunk{
        public int id;
        public byte[] data;
    }

    public static class StreamBuilder{
        public final int id;
        public final Class<? extends Streamable> type;
        public final int total;
        public final ByteArrayOutputStream stream;

        public StreamBuilder(StreamBegin begin){
            id = begin.id;
            type = begin.type;
            total = begin.total;
            stream = new ByteArrayOutputStream();
        }

        public void add(byte[] bytes){
            try {
                stream.write(bytes);
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        public Streamable build(){
            try {
                Streamable s = ClassReflection.newInstance(type);
                s.stream = new ByteArrayInputStream(stream.toByteArray());
                return s;
            }catch(ReflectionException e){
                throw new RuntimeException(e);
            }
        }

        public boolean isDone(){
            return stream.size() >= total;
        }
    }
}
