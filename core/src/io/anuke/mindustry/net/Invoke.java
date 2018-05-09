package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packets.InvokePacket;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.IOUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.playerGroup;

/**Class for invoking static methods of other classes remotely.*/
public class Invoke {
    private static ObjectMap<Class, ObjectMap<String, Method>> methods = new ObjectMap<>();
    private static ObjectMap<String, Class> classes = new ObjectMap<>();
    private static ObjectMap<Method, Boolean> methodLocal = new ObjectMap<>();

    /**
     * Invokes a method remotely (on all clients) and locally.
     * @param type Type of class to invoke method on
     * @param methodName Name of method to invoke. Overloads are not allowed!
     * @param args List of arguments to invoke the method with.
     */
    public static void on(Class<?> type, String methodName, Object... args){
        try {
            Method method = getMethod(type, methodName);
            if(methodLocal.get(method)){
                method.invoke(null, args);
            }
            InvokePacket packet = new InvokePacket();
            packet.args = args;
            packet.type = type;
            packet.method = method;
            packet.args = args;
            Net.send(packet, SendMode.tcp);
        }catch (ReflectionException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Invokes an event in the {@link NetEvents} class by name.
     * @param methodName the name of the event method to invoke
     * @param args the method arguments
     */
    public static void event(String methodName, Object... args){
        on(NetEvents.class, methodName, args);
    }

    //TODO refactor to serializer map!
    static void writeObjects(ByteBuffer buffer, Object[] objects){
        for(Object o : objects){
            Class type = o.getClass();

            if(type == int.class){
                buffer.putInt((Integer)o);
            }else if(type == float.class){
                buffer.putFloat((Float)o);
            }else if(type == short.class){
                buffer.putShort((Short)o);
            }else if(type == boolean.class){
                buffer.put((byte)((Boolean)o ? 1 : 0));
            }else if(type == byte.class){
                buffer.put((Byte)o);
            }else if(type == long.class){
                buffer.putLong((Long)o);
            }else if(type == short.class){
                buffer.putShort((Short)o);
            }else if(type == Tile.class){
                buffer.putInt(((Tile)o).packedPosition());
            }else if(type == Entity.class){
                buffer.put((byte)((Entity)o).getGroup().getID());
                buffer.putInt(((Entity)o).id);
            }else if(type == Player.class){
                buffer.putInt(((Player)o).id);
            }else if(type == Team.class){
                buffer.put((byte)((Team)o).ordinal());
            }else if(type == String.class){
                IOUtils.writeString(buffer, (String)o);
            }
        }
    }

    static Object[] readObjects(ByteBuffer buffer, Class[] types){
        Object[] result = new Object[types.length];
        for (int i = 0; i < result.length; i++) {
            Class type = types[i];
            Object obj = null;

            if(type == int.class){
                obj = buffer.getInt();
            }else if(type == float.class){
                obj = buffer.getFloat();
            }else if(type == short.class){
                obj = buffer.getShort();
            }else if(type == boolean.class){
                obj = buffer.get() == 1;
            }else if(type == byte.class){
                obj = buffer.get();
            }else if(type == long.class){
                obj = buffer.getLong();
            }else if(type == short.class){
                obj = buffer.getShort();
            }else if(type == Tile.class){
                obj = Vars.world.tile(buffer.getInt());
            }else if(type == Entity.class){
                byte group = buffer.get();
                int id = buffer.getInt();
                obj = Entities.getGroup(group).getByID(id);
            }else if(type == Player.class){
                int id = buffer.getInt();
                obj = playerGroup.getByID(id);
            }else if(type == Team.class){
                obj = Team.values()[buffer.get()];
            }else if(type == String.class){
                obj = IOUtils.readString(buffer);
            }

            if(obj != null){
                result[i] = obj;
            }else{
                throw new RuntimeException("Unable to read object of type '" + type + "'!");
            }
        }

        return result;
    }

    static Class findClass(String name) throws ReflectionException{
        Class cl = classes.get(name);
        if(cl == null){
            cl = ClassReflection.forName(name);
            classes.put(name, cl);
        }
        return cl;
    }

    static Method getMethod(Class type, String methodname) throws ReflectionException{
        ObjectMap<String, Method> map = methods.get(type);

        if(map == null){
            map = new ObjectMap<>();
            methods.put(type, map);
        }

        Method method = map.get(methodname);

        if(method == null){
            method = ClassReflection.getMethod(type, methodname, (Class[])null);
            if(method.getDeclaredAnnotation(Remote.class) == null){
                throw new RuntimeException("Attempt to invoke method '" + methodname + "', which is not Invokable!");
            }
            methodLocal.put(method, method.getDeclaredAnnotation(Local.class) != null);
            map.put(methodname, method);

        }

        return method;

    }

    /**Marks a method as invokable remotely with {@link Invoke#on(Class, String, Object...)}*/
    @Retention(RetentionPolicy.RUNTIME)
    @interface Remote{}

    /**Marks a method to be locally invoked as well as remotely invoked.*/
    @Retention(RetentionPolicy.RUNTIME)
    @interface Local{}

}
