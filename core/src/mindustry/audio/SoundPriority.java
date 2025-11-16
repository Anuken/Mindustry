package mindustry.audio;

import arc.*;
import arc.audio.*;
import arc.struct.*;
import mindustry.gen.*;

import static mindustry.gen.Sounds.*;

/** Sets up priorities and groups for various sounds. */
public class SoundPriority{

    public static void init(){
        //priority 2: long weapon loops
        set(
        2f,
        laserbig,
        beam
        );

        //priority 1.5: big weapon sounds, not loops
        set(1.5f,
        railgun,
        largeCannon
        );

        //priority 1: ambient noises
        set(
        1f,
        conveyor,
        smelter,
        drill,
        extractLoop,
        flux,
        hum,
        respawning
        );

        //this is spammed a LOT
        Sounds.shootBig.setMinConcurrentInterrupt(shootBig.getLength() * 0.9f);

        for(var sound : Core.assets.getAll(Sound.class, new Seq<>())){
            sound.setMinConcurrentInterrupt(Math.min(0.25f, sound.getLength() * 0.5f));
            sound.setMaxConcurrent(7);
        }
    }

    static void set(float value, Sound... sounds){
        for(var s : sounds) s.setPriority(value);
    }
}
