package io.anuke.mindustry.game;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;

/** Controls playback of multiple music tracks.*/
public class MusicControl{
    private static final float finTime = 120f, foutTime = 120f;
    private @Nullable Music current;
    private float fade;

    public void play(@Nullable Music music){
        if(current != null){
            current.setVolume(fade * Core.settings.getInt("musicvol") / 100f);
        }

        if(current == null && music != null){
            current = music;
            current.setLooping(true);
            current.setVolume(fade = 0f);
            current.play();
        }else if(current == music && music != null){
            fade = Mathf.clamp(fade + Time.delta()/finTime);
        }else if(current != null){
            fade = Mathf.clamp(fade - Time.delta()/foutTime);

            if(fade <= 0.01f){
                current.stop();
                current = null;
                if(music != null){
                    current = music;
                    current.setVolume(fade = 0f);
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
