package io.anuke.mindustry.net;

import io.anuke.mindustry.net.Packets.StreamBegin;

import java.io.*;

public class Streamable implements Packet{
    public transient ByteArrayInputStream stream;

    @Override
    public boolean isImportant(){
        return true;
    }

    public static class StreamBuilder{
        public final int id;
        public final byte type;
        public final int total;
        public final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        public StreamBuilder(StreamBegin begin){
            id = begin.id;
            type = begin.type;
            total = begin.total;
        }

        public float progress(){
            return (float)stream.size() / total;
        }

        public void add(byte[] bytes){
            try{
                stream.write(bytes);
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }

        public Streamable build(){
            Streamable s = (Streamable)Registrator.getByID(type).constructor.get();
            s.stream = new ByteArrayInputStream(stream.toByteArray());
            return s;
        }

        public boolean isDone(){
            return stream.size() >= total;
        }
    }
}
