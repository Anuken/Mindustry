package io.anuke.kryonet;

import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.FrameworkMessage.*;

import java.nio.ByteBuffer;

public class FrameworkSerializer {

    public static void write(ByteBuffer buffer, FrameworkMessage message){
        if(message instanceof Ping){
            Ping p = (Ping)message;

            buffer.put((byte)0);
            buffer.putInt(p.id);
            buffer.put(p.isReply ? 1 : (byte)0);
        }else if(message instanceof DiscoverHost){
            DiscoverHost p = (DiscoverHost)message;

            buffer.put((byte)1);
        }else if(message instanceof KeepAlive){
            KeepAlive p = (KeepAlive)message;

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

    public static FrameworkMessage read(ByteBuffer buffer){
        byte id = buffer.get();

        if(id == 0){
            Ping p = new Ping();
            p.id = buffer.getInt();
            p.isReply = buffer.get() == 1;

            return p;
        }else if(id == 1){
            DiscoverHost p = new DiscoverHost();

            return p;
        }else if(id == 2){
            KeepAlive p = new KeepAlive();

            return p;
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
