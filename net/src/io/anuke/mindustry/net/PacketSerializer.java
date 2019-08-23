package io.anuke.mindustry.net;

import io.anuke.arc.function.*;
import io.anuke.arc.net.*;
import io.anuke.arc.net.FrameworkMessage.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mnet.*;

import java.nio.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class PacketSerializer implements NetSerializer, MSerializer{
    private ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 8);

    @Override
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

    @Override
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


    public static void writeFramework(ByteBuffer buffer, FrameworkMessage message){
        if(message instanceof Ping){
            Ping p = (Ping)message;
            buffer.put((byte)0);
            buffer.putInt(p.id);
            buffer.put(p.isReply ? 1 : (byte)0);
        }else if(message instanceof DiscoverHost){
            buffer.put((byte)1);
        }else if(message instanceof KeepAlive){
            buffer.put((byte)2);
        }else if(message instanceof RegisterUDP){
            RegisterUDP p = (RegisterUDP)message;
            buffer.put((byte)3);
            buffer.putInt(p.connectionID);
        }else if(message instanceof RegisterTCP){
            RegisterTCP p = (RegisterTCP)message;
            buffer.put((byte)4);
            buffer.putInt(p.connectionID);
        }
    }

    public static FrameworkMessage readFramework(ByteBuffer buffer){
        byte id = buffer.get();

        if(id == 0){
            Ping p = new Ping();
            p.id = buffer.getInt();
            p.isReply = buffer.get() == 1;
            return p;
        }else if(id == 1){
            return new DiscoverHost();
        }else if(id == 2){
            return new KeepAlive();
        }else if(id == 3){
            RegisterUDP p = new RegisterUDP();
            p.connectionID = buffer.getInt();
            return p;
        }else if(id == 4){
            RegisterTCP p = new RegisterTCP();
            p.connectionID = buffer.getInt();
            return p;
        }else{
            throw new RuntimeException("Unknown framework message!");
        }
    }
}
