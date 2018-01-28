package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;

public class ClientDebug {
    private OrderedMap<Class<?>, Long> last = new OrderedMap<>();
    private int syncPlayers = 0;
    private int syncEnemies = 0;

    public void handle(Object packet){
        last.put(packet.getClass(), TimeUtils.millis());
    }

    public void setSyncDebug(int players, int enemies){
        this.syncEnemies = enemies;
        this.syncPlayers = players;
    }

    public String getOut(){
        StringBuilder build = new StringBuilder();
        for(Class<?> type : last.orderedKeys()){
            build.append(elapsed(type));
            build.append("\n");
        }
        build.append("sync.players: ");
        build.append(syncPlayers);
        build.append("\n");
        build.append("sync.enemies: ");
        build.append(syncEnemies);
        build.append("\n");
        return build.toString();
    }

    private String elapsed(Class<?> type){
        long t = last.get(type, -1L);
        if(t == -1){
            return ClassReflection.getSimpleName(type) + ": <never>";
        }else{
            float el = TimeUtils.timeSinceMillis(t) / 1000f;
            String tu;
            if(el > 1f){
                tu = (int)el + "s";
            }else{
                tu = (int)(el * 60) + "f";
            }
            return ClassReflection.getSimpleName(type) + ": " + tu;
        }
    }
}
