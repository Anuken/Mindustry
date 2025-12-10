package mindustry.audio;

import arc.*;
import arc.audio.*;
import arc.struct.*;

import static mindustry.gen.Sounds.*;

/** Sets up priorities and groups for various sounds. */
public class SoundPriority{
    static int lastGroup = 1;

    public static void init(){
        max(7, beamPlasma, shootMeltdown, beamMeltdown);

        //priority 2: long weapon loops and big explosions
        set(
        2f,
        beamMeltdown,
        beamLustre,
        beamPlasma,
        reactorExplosion,
        reactorExplosion2,
        coreExplode,
        blockExplodeElectricBig, blockExplodeExplosive
        );

        //priority 1.5: big weapon sounds, not loops
        set(
        1.5f,
        shootMeltdown,
        shootSublimate,
        shootForeshadow,
        shootConquer,
        shootCorvus,
        chargeCorvus,
        chargeVela,
        chargeLancer
        );

        //priority 1: ambient noises
        set(
        1f,
        loopConveyor,
        loopSmelter,
        loopDrill,
        loopExtract,
        loopFlux,
        loopHum,
        loopBio,
        loopTech,
        loopUnitBuilding
        );

        //very loud
        shootLancer.setMaxConcurrent(5);

        sameGroup(shootFlame, shootFlamePlasma);
        sameGroup(shootMissile, shootMissileShort, shootMissilePlasmaShort);
        sameGroup(shootArc, shootPulsar);

        for(var sound : Core.assets.getAll(Sound.class, new Seq<>())){
            sound.setMinConcurrentInterrupt(Math.min(0.25f, sound.getLength() * 0.5f));
        }

        mechStepSmall.setMinConcurrentInterrupt(0.5f);
        mechStep.setMinConcurrentInterrupt(0.5f);
        walkerStep.setMinConcurrentInterrupt(0.6f);
        mechStepHeavy.setMinConcurrentInterrupt(0.6f);

        shieldHit.setMaxConcurrent(4);

        max(4, mechStep, mechStepHeavy, walkerStep, walkerStepSmall, walkerStepTiny, mechStepSmall);

        //repair sounds are lower priority and generally not important
        set(-1f, blockHeal, healWave);

        //step sounds are low priority
        set(-2f, mechStep, mechStepHeavy, walkerStep, walkerStepSmall, walkerStepTiny, mechStepSmall);

        coreExplode.setFalloffOffset(100f);
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
