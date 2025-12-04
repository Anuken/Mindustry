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
        laserbeam,
        beamPlasma
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

        sameGroup(flame, flamePlasma);
        sameGroup(missile, missileShort, missilePlasmaShort);
        sameGroup(spark, shock);

        for(var sound : Core.assets.getAll(Sound.class, new Seq<>())){
            sound.setMinConcurrentInterrupt(Math.min(0.25f, sound.getLength() * 0.5f));
        }

        mechStep.setMinConcurrentInterrupt(0.5f);
        walkerStep.setMinConcurrentInterrupt(0.6f);
        mechStepHeavy.setMinConcurrentInterrupt(0.6f);

        shieldHit.setMaxConcurrent(4);

        max(4, mechStep, mechStepHeavy, walkerStep);
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
