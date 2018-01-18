package io.anuke.kryonet;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.serialization.Serialization;
import io.anuke.mindustry.net.Packet;
import io.anuke.mindustry.net.Registrator;

import java.nio.ByteBuffer;

public class ByteSerializer implements Serialization {

    @Override
    public void write(ByteBuffer byteBuffer, Object o) {
        if(o instanceof FrameworkMessage){
            byteBuffer.put((byte)-2); //code for framework message
            FrameworkSerializer.write(byteBuffer, (FrameworkMessage)o);
        }else {
            if (!(o instanceof Packet))
                throw new RuntimeException("All sent objects must implement be Packets! Class: " + o.getClass());
            byte id = Registrator.getID(o.getClass());
            if (id == -1)
                throw new RuntimeException("Unregistered class: " + ClassReflection.getSimpleName(o.getClass()));
            byteBuffer.put(id);
            ((Packet) o).write(byteBuffer);
        }
    }

    @Override
    public Object read(ByteBuffer byteBuffer) {
        try {
            byte id = byteBuffer.get();
            if(id == -2){
               return FrameworkSerializer.read(byteBuffer);
            }else {
                Class<?> type = Registrator.getByID(id);
                Packet packet = (Packet) ClassReflection.newInstance(type);
                packet.read(byteBuffer);
                return packet;
            }
        }catch (ReflectionException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getLengthLength() {
        return 2;
    }

    @Override
    public void writeLength(ByteBuffer byteBuffer, int i) {
        byteBuffer.putShort((short)i);
    }

    @Override
    public int readLength(ByteBuffer byteBuffer) {
        return byteBuffer.getShort();
    }
}
