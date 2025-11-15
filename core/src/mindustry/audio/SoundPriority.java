package mindustry.audio;

import arc.*;
import arc.audio.*;
import arc.struct.*;

import static mindustry.gen.Sounds.*;

/** Sets up priorities and groups for various sounds. */
public class SoundPriority{
    static int lastGroup = 1;

    public static void init(){
        max(7, laserbig, beam, laserbeam);

        //priority 2: long weapon loops
        set(
        2f,
        laserbig,
        beam,
        laserbeam
        );

        //priority 1.5: big weapon sounds, not loops
        set(
        1.5f,
        railgun,
        largeCannon,
        lasercharge,
        lasercharge2,
        lasercharge3
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

        //very loud
        laser.setMaxConcurrent(5);

        //sameGroup(hit1, hit2, hit3);
        //max(4, hit1, hit2, hit3);

        sameGroup(missile, missileShort, missilePlasmaShort);

        for(var sound : Core.assets.getAll(Sound.class, new Seq<>())){
            sound.setMinConcurrentInterrupt(Math.min(0.25f, sound.getLength() * 0.5f));
        }

        mechStep.setMinConcurrentInterrupt(0.3f);
        mechStep.setMaxConcurrent(3);
    }

    static void max(int max, Sound... sounds){
        for(var s : sounds) s.setMaxConcurrent(max);
    }

    static void sameGroup(Sound... sounds){
        int id = lastGroup ++;
        for(var s : sounds) s.setConcurrentGroup(id);
    }

    static void set(float value, Sound... sounds){
        for(var s : sounds) s.setPriority(value);
    }
}
