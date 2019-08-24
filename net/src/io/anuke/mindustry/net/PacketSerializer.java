package io.anuke.mindustry.net;

import io.anuke.arc.function.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mnet.*;

import java.nio.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class PacketSerializer implements MSerializer{
    private ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 8);

    public void write(ByteBuffer byteBuffer, Object o){
        if(o == null){
            byteBuffer.put((byte)-1);
            return;
        }

        if (!(o instanceof Packet))
            throw new RuntimeException("All sent objects must implement be Packets! Class: " + o.getClass());
        byte id = Registrator.getID(o.getClass());
        if (id == -1)
            throw new RuntimeException("Unregistered class: " + o.getClass());
        byteBuffer.put(id);
        ((Packet) o).write(byteBuffer);
    }

    public Object read(ByteBuffer byteBuffer){
        byte id = byteBuffer.get();
        if(id == -1){
            return null;
        }
        Packet packet = Pools.obtain((Class<Packet>) Registrator.getByID(id).type, (Supplier<Packet>) Registrator.getByID(id).constructor);
        packet.read(byteBuffer);
        return packet;
    }

    @Override
    public byte[] serialize(Object o){
        buffer.position(0);
        write(buffer, o);
        return Arrays.copyOfRange(buffer.array(), 0, buffer.position());
    }

    @Override
    public byte[] serialize(Object o, int offset){
        buffer.position(0);
        write(buffer, o);
        int length = buffer.position();
        byte[] bytes = new byte[length + offset];
        System.arraycopy(buffer.array(), 0, bytes, offset, length);
        return bytes;
    }

    @Override
    public int serialize(Object o, byte[] bytes, int offset){
        buffer.position(0);
        write(buffer, o);
        int length = buffer.position();
        System.arraycopy(buffer.array(), 0, bytes, offset, length);
        return length;
    }

    @Override
    public Object deserialize(byte[] bytes){
        buffer.position(0);
        buffer.put(bytes);
        buffer.position(0);
        return read(buffer);
    }

    @Override
    public Object deserialize(byte[] bytes, int offset, int length){
        buffer.position(0);
        buffer.put(bytes, offset, length);
        buffer.position(0);
        return read(buffer);
    }
}
