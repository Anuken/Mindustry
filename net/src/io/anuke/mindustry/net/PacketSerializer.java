package io.anuke.mindustry.net;

import io.anuke.arc.function.Supplier;
import io.anuke.arc.net.FrameworkMessage;
import io.anuke.arc.net.FrameworkMessage.*;
import io.anuke.arc.net.NetSerializer;
import io.anuke.arc.util.pooling.Pools;

import java.nio.ByteBuffer;

@SuppressWarnings("unchecked")
public class PacketSerializer implements NetSerializer{

    @Override
    public void write(ByteBuffer byteBuffer, Object o){
        if(o instanceof FrameworkMessage){
            byteBuffer.put((byte)-2); //code for framework message
            writeFramework(byteBuffer, (FrameworkMessage)o);
        }else{
            if(!(o instanceof Packet))
                throw new RuntimeException("All sent objects must implement be Packets! Class: " + o.getClass());
            byte id = Registrator.getID(o.getClass());
            if(id == -1)
                throw new RuntimeException("Unregistered class: " + o.getClass());
            byteBuffer.put(id);
            ((Packet)o).write(byteBuffer);
        }
    }

    @Override
    public Object read(ByteBuffer byteBuffer){
        byte id = byteBuffer.get();
        if(id == -2){
            return readFramework(byteBuffer);
        }else{
            Packet packet = Pools.obtain((Class<Packet>)Registrator.getByID(id).type, (Supplier<Packet>)Registrator.getByID(id).constructor);
            packet.read(byteBuffer);
            return packet;
        }
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
