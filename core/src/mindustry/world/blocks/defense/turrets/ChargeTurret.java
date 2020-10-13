package mindustry.world.blocks.defense.turrets;

import arc.audio.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class ChargeTurret extends PowerTurret{
    public float chargeTime = 30f;
    public int chargeEffects = 5;
    public float chargeMaxDelay = 10f;
    public Effect chargeEffect = Fx.none;
    public Effect chargeBeginEffect = Fx.none;
    public Sound chargeSound = Sounds.none;

    public ChargeTurret(String name){
        super(name);
    }

    public class ChargeTurretBuild extends PowerTurretBuild{
        public boolean shooting;
        public int chargeCounter;

        @Override
        public void shoot(BulletType ammo){
            useAmmo();
            for(int i = 0; i < shots; i++){
                final int indexC = i;
                Time.run(burstSpacing * i, () -> {
                    tr.trns(rotation, size * tilesize / 2f + (barrelPos.length != 0f ? barrelPos[chargeCounter % barrels][1] : 0f), (barrelPos.length != 0f ? barrelPos[chargeCounter % barrels][0] : 0f));
                    chargeBeginEffect.at(x + tr.x, y + tr.y, rotation + (barrelPos.length != 0f ? barrelPos[chargeCounter % barrels][2] : 0f) + (barrelBurst ? (indexC - (int)(shots / 2f)) * spread : 0));
                    chargeSound.at(x + tr.x, y + tr.y, 1);
                    
                    for(int j = 0; j < chargeEffects; j++){
                        Time.run(Mathf.random(chargeMaxDelay), () -> {
                            if(!isValid()) return;
                            tr.trns(rotation, size * tilesize / 2f + (barrelPos.length != 0f ? barrelPos[chargeCounter % barrels][1] : 0f), (barrelPos.length != 0f ? barrelPos[chargeCounter % barrels][0] : 0f));
                            chargeEffect.at(x + tr.x, y + tr.y, rotation + (barrelPos.length != 0f ? barrelPos[chargeCounter % barrels][2] : 0f) + (barrelBurst ? (indexC - (int)(shots / 2f)) * spread : 0));
                        });
                    }
                    if(!barrelBurst){
                        chargeCounter++;
                    }
                });
            }
            if(barrelBurst){
                chargeCounter++;
            }

            shooting = true;

            Time.run(chargeTime, () -> {
                for(int i = 0; i < shots; i++){
                    final int indexS = i;
                    Time.run(burstSpacing * i, () -> {
                        if(!isValid()) return;
                        tr.trns(rotation, size * tilesize / 2f + (barrelPos.length != 0f ? barrelPos[shotCounter % barrels][1] : 0f), (barrelPos.length != 0f ? barrelPos[shotCounter % barrels][0] : 0f));
                        recoil = recoilAmount;
                        heat = 1f;
                        bullet(ammo, rotation + Mathf.range(inaccuracy + ammo.inaccuracy) + (barrelPos.length != 0f ? barrelPos[shotCounter % barrels][2] : 0f) + (barrelBurst ? (indexS - (int)(shots / 2f)) * spread : 0));
                        effects();
                        if(!barrelBurst){
                            shotCounter++;
                        }
                    });
                }
                if(barrelBurst){
                    shotCounter++;
                }
                shooting = false;
            });
        }

        @Override
        public boolean shouldTurn(){
            return !shooting;
        }
    }
}
