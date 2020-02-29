package mindustry.game;

import arc.*;
import arc.util.*;

public class Universe{
    private long seconds;
    private float secondCounter;

    public Universe(){
        load();
    }

    public void update(){
        secondCounter += Time.delta() / 60f;
        if(secondCounter >= 1){
            seconds += (int)secondCounter;
            secondCounter %= 1f;

            //save every few seconds
            if(seconds % 10 == 1){
                save();
            }
        }
    }

    public float secondsMod(float mod, float scale){
        return (seconds / scale) % mod;
    }

    public long seconds(){
        return seconds;
    }

    public float secondsf(){
        return seconds + secondCounter;
    }

    private void save(){
        Core.settings.put("utime", seconds);
        Core.settings.save();
    }

    private void load(){
        seconds = Core.settings.getLong("utime");
    }

}
