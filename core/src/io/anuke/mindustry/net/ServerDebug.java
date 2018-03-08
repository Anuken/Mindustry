package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.playerGroup;

public class ServerDebug {
    private IntMap<OrderedMap<Class<?>, Long>> last = new IntMap<>();

    public void handle(int connection, Object packet){
        try {
            if (!last.containsKey(connection))
                last.put(connection, new OrderedMap<>());
            if (packet instanceof Disconnect)
                last.remove(connection);
            else
                last.get(connection).put(packet.getClass(), TimeUtils.millis());
        }catch (Exception e){
            Log.err("<An internal debug error has occurred.>");
        }
    }

    public String getOut(){
        StringBuilder build = new StringBuilder();
        for(Player player : playerGroup.all()){
            OrderedMap<Class<?>, Long> map = last.get(player.clientid, new OrderedMap<>());
            build.append("connection ");
            build.append(player.clientid);
            build.append(" / player '");
            build.append(player.name);
            build.append(" android: ");
            build.append(player.isAndroid);
            build.append("'\n");

            for(Class<?> type : map.orderedKeys()){
                build.append("   ");
                build.append(elapsed(type, map));
                build.append("\n");
            }
        }
        return build.toString();
    }

    private String elapsed(Class<?> type, OrderedMap<Class<?>, Long> last) {
        long t = last.get(type, -1L);
        if (t == -1) {
            return ClassReflection.getSimpleName(type) + ": <never>";
        } else {
            float el = TimeUtils.timeSinceMillis(t) / 1000f;
            String tu;
            if (el > 1f) {
                tu = (int) el + "s";
            } else {
                tu = (int) (el * 60) + "f";
            }
            return ClassReflection.getSimpleName(type) + ": " + tu;
        }
    }
}
