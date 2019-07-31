package io.anuke.mindustry.game;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;

public class MusicControl{
    private static final float finTime = 80f, foutTime = 80f;
    private @Nullable Music current;

    public void play(@Nullable Music music){
        if(current == null && music != null){
            current = music;
            current.setLooping(true);
            current.setVolume(0f);
            current.play();
        }else if(current == music && music != null){
            current.setVolume(Mathf.clamp(current.getVolume() + Time.delta()/finTime));
        }else if(current != null){
            current.setVolume(Mathf.clamp(current.getVolume() - Time.delta()/foutTime));

            if(current.getVolume() <= 0.01f){
                current.stop();
                current = null;
                if(music != null){
                    current = music;
                    current.setVolume(0f);
                    current.setLooping(true);
                    current.play();
                }
            }
        }
    }

    public void silence(){
        play(null);
    }
}
