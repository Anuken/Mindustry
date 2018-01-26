package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.anuke.mindustry.net.Packet.ImportantPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Streamable implements ImportantPacket{
    public transient ByteArrayInputStream stream;

    /**Marks the beginning of a stream.*/
    public static class StreamBegin implements Packet{
        private static int lastid;

        public int id = lastid ++;
        public int total;
        public Class<? extends Streamable> type;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(id);
            buffer.putInt(total);
            buffer.put(Registrator.getID(type));
        }

        @Override
        public void read(ByteBuffer buffer) {
            id = buffer.getInt();
            total = buffer.getInt();
            type = (Class<? extends Streamable>)Registrator.getByID(buffer.get());
        }
    }

    public static class StreamChunk implements Packet{
        public int id;
        public byte[] data;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(id);
            buffer.putShort((short)data.length);
            buffer.put(data);
        }

        @Override
        public void read(ByteBuffer buffer) {
            id = buffer.getInt();
            data = new byte[buffer.getShort()];
            buffer.get(data);
        }
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
